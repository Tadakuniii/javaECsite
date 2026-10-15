package com.example.demo;

public record ProductResponse(
        Long id,
        String productCode,
        String name,
        String category,
        String description,
        String size,
        int price,
        int stockQuantity,
        String imageUrl,
        String label,
        String accentColor,
        String illustration) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getProductCode(), product.getName(),
                product.getCategory().getName(), product.getDescription(), product.getSizeDescription(),
                product.getPrice(), product.getStockQuantity(), product.getImageUrl(),
                product.getDisplayLabel(), product.getAccentColor(), product.getIllustrationType());
    }
}
