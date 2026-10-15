package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer {

    @Id
    @Column(length = 64)
    private String customerId;
    @Column(nullable = false)
    private String customerPassword;
    @Column(nullable = false, length = 100)
    private String customerName;
    @Column(unique = true, length = 255)
    private String email;
    // 既存行を補完してからCustomerCreatedAtMigrationがDBのNOT NULL制約を適用する。
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Customer() {}

    public Customer(String customerId, String customerPassword, String customerName) {
        this.customerId = customerId;
        this.customerPassword = customerPassword;
        this.customerName = customerName;
    }

    @PrePersist
    void recordCreationTime() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
