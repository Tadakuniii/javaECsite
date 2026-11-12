package com.example.demo;

import java.util.List;

public record ShoppingCartResponse(List<CartItemResponse> items, int totalQuantity, int totalPrice) {
    static ShoppingCartResponse from(List<CartItem> cartItems) {
        List<CartItemResponse> items = cartItems.stream().map(CartItemResponse::from).toList();
        int totalQuantity = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        int totalPrice = cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity()).sum();
        return new ShoppingCartResponse(items, totalQuantity, totalPrice);
    }
}
