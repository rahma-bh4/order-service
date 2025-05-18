package com.project.orderservice.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.orderservice.dto.InvoiceDto;
import com.project.orderservice.service.InvoiceService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDto> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<InvoiceDto> getInvoiceByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByOrderId(orderId));
    }
    
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<InvoiceDto> getInvoiceByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByInvoiceNumber(invoiceNumber));
    }
    
    @GetMapping
    public ResponseEntity<List<InvoiceDto>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<InvoiceDto>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(invoiceService.getInvoicesByDateRange(startDate, endDate));
    }
    
    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<InvoiceDto> updateInvoicePaymentStatus(
            @PathVariable Long id,
            @RequestParam String paymentStatus) {
        return ResponseEntity.ok(invoiceService.updateInvoicePaymentStatus(id, paymentStatus));
    }
}
