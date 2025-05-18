package com.project.orderservice.dto;



import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InvoiceDto {
    private Long id;
    
    private String invoiceNumber;
    
    private Long orderId;
    
    private LocalDateTime issueDate;
    
    private LocalDateTime dueDate;
    
    private Double totalAmount;
    
    private Double taxAmount;
    
    private String paymentStatus;
}