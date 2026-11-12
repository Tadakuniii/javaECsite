package com.example.demo;

public record OrderItemResponse(
        Long productId, String productName, String size, int unitPrice, int quantity, int subtotal,
        String imageUrl, String label, String accentColor, String illustration) {

    static OrderItemResponse from(OrderItem item) {
        Product product = item.getProduct();
        return new OrderItemResponse(
                product.getId(), item.getProductName(),
                item.getSizeDescription() != null ? item.getSizeDescription() : product.getSizeDescription(), item.getUnitPrice(),
                item.getQuantity(), item.getUnitPrice() * item.getQuantity(), product.getImageUrl(),
                product.getDisplayLabel(), product.getAccentColor(), product.getIllustrationType());
    }
}
