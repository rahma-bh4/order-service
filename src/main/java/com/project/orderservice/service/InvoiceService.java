package com.project.orderservice.service;



import java.time.LocalDateTime;
import java.util.List;

import com.project.orderservice.dto.InvoiceDto;
import com.project.orderservice.model.Invoice;
import com.project.orderservice.model.Order;

public interface InvoiceService {
    InvoiceDto getInvoiceById(Long id);
    
    InvoiceDto getInvoiceByOrderId(Long orderId);
    
    InvoiceDto getInvoiceByInvoiceNumber(String invoiceNumber);
    
    List<InvoiceDto> getAllInvoices();
    
    List<InvoiceDto> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    Invoice generateInvoice(Order order);
    
    InvoiceDto updateInvoicePaymentStatus(Long id, String paymentStatus);
}