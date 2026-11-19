package com.example.demo;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "purchase_orders", check = {
        @CheckConstraint(name = "chk_purchase_order_quantity", constraint = "quantity > 0"),
        @CheckConstraint(name = "chk_purchase_order_unit_cost", constraint = "unit_cost >= 0")
})
@Getter
@Setter
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 200)
    private String supplierName;

    @Column(nullable = false)
    private LocalDate expectedDeliveryDate;

    @Column(nullable = false)
    private int unitCost;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean automatic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.ORDERED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime receivedAt;

    protected PurchaseOrder() {}

    public PurchaseOrder(Product product, String supplierName, LocalDate expectedDeliveryDate,
                         int unitCost, int quantity, boolean automatic, LocalDateTime orderedAt) {
        this.product = product;
        this.productName = product.getName();
        this.supplierName = supplierName;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.unitCost = unitCost;
        this.quantity = quantity;
        this.automatic = automatic;
        this.orderedAt = orderedAt;
    }
}
