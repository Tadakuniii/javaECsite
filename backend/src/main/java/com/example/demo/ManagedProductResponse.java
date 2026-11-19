package com.example.demo;

public record ManagedProductResponse(
        Long id,
        String productCode,
        String name,
        String category,
        String description,
        String sizeDescription,
        int price,
        int stockQuantity,
        String imageUrl,
        boolean active,
        boolean autoOrderEnabled,
        int reorderThreshold,
        int reorderQuantity,
        String defaultSupplierName,
        int defaultOrderUnitCost,
        int defaultLeadTimeDays) {

    static ManagedProductResponse from(Product product) {
        return new ManagedProductResponse(product.getId(), product.getProductCode(), product.getName(),
                product.getCategory().getName(), product.getDescription(), product.getSizeDescription(), product.getPrice(),
                product.getStockQuantity(), product.getImageUrl(), product.isActive(), product.isAutoOrderEnabled(),
                product.getReorderThreshold(), product.getReorderQuantity(), product.getDefaultSupplierName(),
                product.getDefaultOrderUnitCost(), product.getDefaultLeadTimeDays());
    }
}
