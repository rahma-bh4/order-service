package com.project.orderservice.dto;



import lombok.Data;

@Data
public class CustomerDto {
    private Long id;
    
    private String firstName;
    
    private String lastName;
    
    private String email;
    
    private String phoneNumber;
    
    private String address;
    
    private String city;
    
    private String country;
    
    private String postalCode;
}