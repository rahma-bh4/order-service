package com.project.orderservice.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import com.project.orderservice.model.OrderItem;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    
    List<OrderItem> findByProductId(Long productId);
}