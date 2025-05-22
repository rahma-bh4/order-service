package com.project.orderservice.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.project.orderservice.dto.CustomerDto;
import com.project.orderservice.dto.LoyaltyActivityDto;

@FeignClient(name = "customer-service")
public interface CustomerClient {
    @GetMapping("/api/customers/{id}")
    CustomerDto getCustomerById(@PathVariable Long id);
    
    @PostMapping("/api/customers/{customerId}/loyalty/points")
    CustomerDto addLoyaltyPoints(@PathVariable Long customerId, @RequestBody LoyaltyActivityDto activityDto);

}
