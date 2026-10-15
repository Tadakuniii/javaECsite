package com.example.demo;

public record ManagedProductResponse(
        Long id,
        String productCode,
        String name,
        String category,
        String description,
        int price,
        int stockQuantity,
        String imageUrl,
        boolean active) {

    static ManagedProductResponse from(Product product) {
        return new ManagedProductResponse(product.getId(), product.getProductCode(), product.getName(),
                product.getCategory().getName(), product.getDescription(), product.getPrice(),
                product.getStockQuantity(), product.getImageUrl(), product.isActive());
    }
}
