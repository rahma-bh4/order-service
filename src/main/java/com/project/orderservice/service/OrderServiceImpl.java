package com.project.orderservice.service;

import feign.FeignException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.orderservice.client.CustomerClient;
import com.project.orderservice.client.ProductClient;
import com.project.orderservice.dto.CustomerDto;
import com.project.orderservice.dto.LoyaltyActivityDto;
import com.project.orderservice.dto.LoyaltyActivityType;
import com.project.orderservice.dto.OrderDto;
import com.project.orderservice.dto.OrderItemDto;
import com.project.orderservice.dto.ProductDto;
import com.project.orderservice.exception.ResourceNotFoundException;
import com.project.orderservice.model.Order;
import com.project.orderservice.model.OrderItem;
import com.project.orderservice.model.OrderStatus;
import com.project.orderservice.repository.OrderItemRepository;
import com.project.orderservice.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;
    private final CustomerClient customerClient;
    private final InvoiceService invoiceService;
    
    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductClient productClient,
            CustomerClient customerClient,
            InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productClient = productClient;
        this.customerClient = customerClient;
        this.invoiceService = invoiceService;
    }
    
    @Override
    @Transactional
    public OrderDto createOrder(OrderDto orderDto) {
        // Validate customer exists
        CustomerDto customer;
        try {
            customer = customerClient.getCustomerById(orderDto.getCustomerId());
        } catch (FeignException e) {
            throw new ResourceNotFoundException("Customer not found with id: " + orderDto.getCustomerId());
        }
        
        // Create order entity
        Order order = new Order();
        order.setCustomerId(orderDto.getCustomerId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        
        // Process order items
        double totalAmount = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (OrderItemDto itemDto : orderDto.getItems()) {
            // Validate product exists and has sufficient stock
            ProductDto product;
            try {
                product = productClient.getProductById(itemDto.getProductId());
            } catch (FeignException e) {
                throw new ResourceNotFoundException("Product not found with id: " + itemDto.getProductId());
            }
            
            if (product.getQuantity() < itemDto.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }
            
            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.calculateSubtotal();
            
            orderItems.add(orderItem);
            totalAmount += orderItem.getSubtotal();
            
            // Update product stock
            product.setQuantity(product.getQuantity() - itemDto.getQuantity());
            productClient.updateProduct(product.getId(), product);
        }
        if (orderDto.getDiscountPercentage() != null && orderDto.getDiscountPercentage() > 0) {
            // Calculate discount amount
            double discountAmount = orderDto.getDiscountAmount() != null ? 
                orderDto.getDiscountAmount() : totalAmount * orderDto.getDiscountPercentage();
            
            // Apply discount
            totalAmount -= discountAmount;
            
            // Store the discount info
            order.setDiscountPercentage(orderDto.getDiscountPercentage());
            order.setDiscountAmount(discountAmount);
        }
        
        // Set order items and total amount
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        
        // Set order reference for items and save them
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }
        
        // Generate invoice
        invoiceService.generateInvoice(savedOrder);
        
        // Map back to DTO
        OrderDto responseDto = mapToDto(savedOrder);
        responseDto.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
        
        return responseDto;
    }
    
    @Override
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        OrderDto orderDto = mapToDto(order);
        
        // Enrich with customer name
        try {
            CustomerDto customer = customerClient.getCustomerById(order.getCustomerId());
            orderDto.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
        } catch (FeignException e) {
            orderDto.setCustomerName("Unknown Customer");
        }
        
        return orderDto;
    }
    
    @Override
    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    private int calculateLoyaltyPoints(double orderTotal) {
        // For example, 1 point per dollar spent
        return (int) orderTotal;
    }
    @Override
    @Transactional
    public OrderDto updateOrder(Long id, OrderDto orderDto) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Cannot update a completed order
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a completed or cancelled order");
        }
        
        // Update order properties
        if (orderDto.getStatus() != null) {
            order.setStatus(orderDto.getStatus());
        }
        
        // Save updated order
        Order updatedOrder = orderRepository.save(order);
        
        return mapToDto(updatedOrder);
    }
    
    @Override
    @Transactional
    public OrderDto updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Additional validation if needed
        if (order.getStatus() == OrderStatus.CANCELLED && status != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot change status of a cancelled order");
        }
        
        if (order.getStatus() == OrderStatus.DELIVERED && status != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot change status of a delivered order");
        }
       
        
        
        // Validate status transition
        validateStatusTransition(order.getStatus(), status);
        OrderStatus oldStatus = order.getStatus();
        // Update the status
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        if (status == OrderStatus.DELIVERED && oldStatus != OrderStatus.DELIVERED) {
            try {
                // Calculate points
                int points = calculateLoyaltyPoints(order.getTotalAmount());
                
                // Create activity dto
                LoyaltyActivityDto activityDto = new LoyaltyActivityDto();
                activityDto.setType(LoyaltyActivityType.PURCHASE);
                activityDto.setPoints(points);
                activityDto.setAmount(order.getTotalAmount());
                activityDto.setDescription("Points awarded for order #" + order.getId());
                activityDto.setReferenceId(order.getId().toString());
                
                // Call customer service to award points
                customerClient.addLoyaltyPoints(order.getCustomerId(), activityDto);
                
            } catch (FeignException e) {
                // Log error but don't fail the order status update
                System.err.println("Error awarding loyalty points: " + e.getMessage());
            }
        }
        // If we're marking as delivered, we might want to update invoice as well
        if (status == OrderStatus.DELIVERED) {
            try {
                invoiceService.updateInvoicePaymentStatus(
                    updatedOrder.getInvoice().getId(), 
                    "PAID"
                );
            } catch (Exception e) {
                // Log but don't fail the order status update
                System.out.println("Warning: Could not update invoice payment status: " + e.getMessage());
            }
        }
        
        OrderDto orderDto = mapToDto(updatedOrder);
        
        // Enrich with customer name if possible
        try {
            CustomerDto customer = customerClient.getCustomerById(order.getCustomerId());
            orderDto.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
        } catch (Exception e) {
            orderDto.setCustomerName("Unknown Customer");
        }
        
        return orderDto;
    }
    
    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        // Only pending orders can be deleted
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be deleted");
        }
        
        // Restore product quantities
        for (OrderItem item : order.getItems()) {
            try {
                ProductDto product = productClient.getProductById(item.getProductId());
                product.setQuantity(product.getQuantity() + item.getQuantity());
                productClient.updateProduct(product.getId(), product);
            } catch (FeignException e) {
                // Log warning but continue with deletion
                System.out.println("Warning: Could not restore quantity for product: " + item.getProductId());
            }
        }
        
        // Delete order (will cascade to order items and invoice)
        orderRepository.delete(order);
    }
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Implement your business rules for valid status transitions
        // For example:
        switch (currentStatus) {
            case PENDING:
                if (newStatus != OrderStatus.PROCESSING && newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException("From PENDING, order can only move to PROCESSING or CANCELLED");
                }
                break;
            case PROCESSING:
                if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException("From PROCESSING, order can only move to SHIPPED or CANCELLED");
                }
                break;
            case SHIPPED:
                if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException("From SHIPPED, order can only move to DELIVERED or CANCELLED");
                }
                break;
            case DELIVERED:
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new IllegalStateException("DELIVERED is a final status");
                }
                break;
            case CANCELLED:
                if (newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException("CANCELLED is a final status");
                }
                break;
        }
    }
    
    @Override
    public List<OrderDto> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderDto> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderDto> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = orderRepository.findByOrderDateBetween(startDate, endDate);
        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    private OrderDto mapToDto(Order order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setCustomerId(order.getCustomerId());
        orderDto.setOrderDate(order.getOrderDate());
        orderDto.setStatus(order.getStatus());
        orderDto.setTotalAmount(order.getTotalAmount());
        
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> {
                    OrderItemDto itemDto = new OrderItemDto();
                    itemDto.setId(item.getId());
                    itemDto.setProductId(item.getProductId());
                    itemDto.setProductName(item.getProductName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setPrice(item.getPrice());
                    itemDto.setSubtotal(item.getSubtotal());
                    return itemDto;
                })
                .collect(Collectors.toList());
        
        orderDto.setItems(itemDtos);
        
        return orderDto;
    }
}