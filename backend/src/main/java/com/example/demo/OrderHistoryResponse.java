package com.example.demo;

import java.time.LocalDateTime;
import java.util.List;

public record OrderHistoryResponse(
        Long id, String status, String statusLabel, int totalAmount,
        LocalDateTime orderedAt, List<OrderItemResponse> items) {

    static OrderHistoryResponse from(CustomerOrder order, List<OrderItem> items) {
        return new OrderHistoryResponse(
                order.getId(), order.getStatus().name(), statusLabel(order.getStatus()),
                order.getTotalAmount(), order.getOrderedAt(),
                items.stream().map(OrderItemResponse::from).toList());
    }

    private static String statusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "手続き中";
            case PAID -> "購入済み";
            case SHIPPED -> "発送済み";
            case COMPLETED -> "完了";
            case CANCELLED -> "キャンセル";
        };
    }
}
