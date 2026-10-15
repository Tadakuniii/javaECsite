package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders",
        check = @CheckConstraint(name = "chk_order_total", constraint = "total_amount >= 0"))
@Getter
@Setter
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    protected CustomerOrder() {}

    public CustomerOrder(Customer customer, int totalAmount) {
        this.customer = customer;
        this.totalAmount = totalAmount;
    }

    @PrePersist
    void recordOrderTime() {
        if (orderedAt == null) orderedAt = LocalDateTime.now();
    }
}
