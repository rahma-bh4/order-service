package com.project.orderservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.project.orderservice.dto.ProductDto;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable Long id);
    
    @PutMapping("/api/products/{id}")
    ProductDto updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto);
}