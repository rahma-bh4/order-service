package com.project.orderservice.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.project.orderservice.model.OrderStatus;

@Data
public class OrderDto {
    private Long id;
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    private String customerName;
    
    private LocalDateTime orderDate = LocalDateTime.now();
    
    private OrderStatus status = OrderStatus.PENDING;
    
    private Double totalAmount;
    
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemDto> items = new ArrayList<>();
}