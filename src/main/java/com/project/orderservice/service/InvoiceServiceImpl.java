package com.project.orderservice.service;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.orderservice.dto.InvoiceDto;
import com.project.orderservice.exception.ResourceNotFoundException;
import com.project.orderservice.model.Invoice;
import com.project.orderservice.model.Order;
import com.project.orderservice.repository.InvoiceRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    
    @Autowired
    public InvoiceServiceImpl(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }
    
    @Override
    public InvoiceDto getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        
        return mapToDto(invoice);
    }
    
    @Override
    public InvoiceDto getInvoiceByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for order with id: " + orderId));
        
        return mapToDto(invoice);
    }
    
    @Override
    public InvoiceDto getInvoiceByInvoiceNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with invoice number: " + invoiceNumber));
        
        return mapToDto(invoice);
    }
    
    @Override
    public List<InvoiceDto> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();
        return invoices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<InvoiceDto> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Invoice> invoices = invoiceRepository.findByIssueDateBetween(startDate, endDate);
        return invoices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public Invoice generateInvoice(Order order) {
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber(generateInvoiceNumber(order));
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now().plusDays(30)); // 30 days payment term
        
        // Use the order's total amount as subtotal
        Double subtotal = order.getTotalAmount();
        invoice.setTotalAmount(subtotal);
        
        // Calculate tax (example: 15% VAT)
        double taxRate = 0.15;
        Double taxAmount = subtotal * taxRate;
        invoice.setTaxAmount(taxAmount);
        
        // FIXED: Calculate the correct total amount (subtotal + tax)
        invoice.setTotalAmount(subtotal + taxAmount);
        
        invoice.setPaymentStatus("UNPAID");
        
        return invoiceRepository.save(invoice);
    }
    
    @Override
    @Transactional
    public InvoiceDto updateInvoicePaymentStatus(Long id, String paymentStatus) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        
        invoice.setPaymentStatus(paymentStatus);
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        
        return mapToDto(updatedInvoice);
    }
    
    private String generateInvoiceNumber(Order order) {
        // Generate invoice number format: INV-YYYYMMDD-ORDERID
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return "INV-" + order.getOrderDate().format(formatter) + "-" + order.getId();
    }
    
    private InvoiceDto mapToDto(Invoice invoice) {
        InvoiceDto invoiceDto = new InvoiceDto();
        BeanUtils.copyProperties(invoice, invoiceDto);
        invoiceDto.setOrderId(invoice.getOrder().getId());
        return invoiceDto;
    }
}