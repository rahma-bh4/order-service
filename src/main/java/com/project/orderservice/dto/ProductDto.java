package com.project.orderservice.dto;



import lombok.Data;

@Data
public class ProductDto {
    private Long id;
    
    private String name;
    
    private String description;
    
    private Double price;
    
    private Integer quantity;
    
    private String category;
    
    private String imageUrl;
    
    private String barcode;
}