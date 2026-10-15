package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products", check = {
        @CheckConstraint(name = "chk_product_price", constraint = "price >= 0"),
        @CheckConstraint(name = "chk_product_stock", constraint = "stock_quantity >= 0")
})
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String productCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, length = 100)
    private String sizeDescription;

    @Column(nullable = false, length = 100)
    private String displayLabel;

    @Column(nullable = false, length = 20)
    private String accentColor;

    @Column(nullable = false, length = 50)
    private String illustrationType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {}

    public Product(String productCode, ProductCategory category, String name, String description,
                   int price, int stockQuantity, String sizeDescription, String displayLabel,
                   String accentColor, String illustrationType) {
        this.productCode = productCode;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.sizeDescription = sizeDescription;
        this.displayLabel = displayLabel;
        this.accentColor = accentColor;
        this.illustrationType = illustrationType;
    }

    @PrePersist
    void recordCreationTime() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void recordUpdateTime() {
        updatedAt = LocalDateTime.now();
    }
}
