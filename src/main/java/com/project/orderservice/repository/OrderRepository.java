package com.project.orderservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.project.orderservice.model.Order;
import com.project.orderservice.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}