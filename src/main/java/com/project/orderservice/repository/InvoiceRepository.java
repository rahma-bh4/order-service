package com.project.orderservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.project.orderservice.model.Invoice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrderId(Long orderId);
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByIssueDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}