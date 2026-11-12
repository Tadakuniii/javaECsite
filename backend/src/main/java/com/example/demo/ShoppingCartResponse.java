package com.example.demo;

import java.util.List;

public record ShoppingCartResponse(List<CartItemResponse> items, long totalQuantity, long totalPrice) {
    static ShoppingCartResponse from(List<CartItem> cartItems) {
        List<CartItemResponse> items = cartItems.stream().map(CartItemResponse::from).toList();
        long totalQuantity = cartItems.stream().mapToLong(CartItem::getQuantity).sum();
        long totalPrice = cartItems.stream()
                .mapToLong(item -> (long) item.getProduct().getPrice() * item.getQuantity()).sum();
        return new ShoppingCartResponse(items, totalQuantity, totalPrice);
    }
}
