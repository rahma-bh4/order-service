package com.project.orderservice.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoyaltyActivityDto {
    private Long id;
    private Long customerId;
    private LoyaltyActivityType type;
    private Integer points;
    private Double amount;
    private String description;
    private String referenceId;
    private LocalDateTime createdAt;
}
