package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer {

    @Id
    private String customerId;
    private String customerPassword;
    private String customerName;

    public Customer() {}

    public Customer(String customerId, String customerPassword, String customerName) {
        this.customerId = customerId;
        this.customerPassword = customerPassword;
        this.customerName = customerName;
    }
}
