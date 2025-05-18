package com.project.orderservice.service;



import java.time.LocalDateTime;
import java.util.List;

import com.project.orderservice.dto.OrderDto;
import com.project.orderservice.model.OrderStatus;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDto);
    
    OrderDto getOrderById(Long id);
    
    List<OrderDto> getAllOrders();
    
    OrderDto updateOrder(Long id, OrderDto orderDto);
    
    OrderDto updateOrderStatus(Long id, OrderStatus status);
    
    void deleteOrder(Long id);
    
    List<OrderDto> getOrdersByCustomerId(Long customerId);
    
    List<OrderDto> getOrdersByStatus(OrderStatus status);
    
    List<OrderDto> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
