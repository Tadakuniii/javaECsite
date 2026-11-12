package com.example.demo;

public record CartItemResponse(
        Long id,
        Long productId,
        String name,
        String size,
        int unitPrice,
        int quantity,
        int stockQuantity,
        String imageUrl,
        String label,
        String accentColor,
        String illustration) {

    static CartItemResponse from(CartItem item) {
        Product product = item.getProduct();
        return new CartItemResponse(item.getId(), product.getId(), product.getName(),
                product.getSizeDescription(), product.getPrice(), item.getQuantity(),
                product.getStockQuantity(), product.getImageUrl(), product.getDisplayLabel(),
                product.getAccentColor(), product.getIllustrationType());
    }
}
