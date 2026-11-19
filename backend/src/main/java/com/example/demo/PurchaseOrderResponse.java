package com.example.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PurchaseOrderResponse(
        Long id, Long productId, String productName, String supplierName,
        LocalDate expectedDeliveryDate, int unitCost, int quantity, long totalCost,
        boolean automatic, String status, String statusLabel,
        LocalDateTime orderedAt, LocalDateTime receivedAt) {

    static PurchaseOrderResponse from(PurchaseOrder order) {
        return new PurchaseOrderResponse(
                order.getId(), order.getProduct().getId(), order.getProductName(), order.getSupplierName(),
                order.getExpectedDeliveryDate(), order.getUnitCost(), order.getQuantity(),
                (long) order.getUnitCost() * order.getQuantity(), order.isAutomatic(), order.getStatus().name(),
                order.getStatus() == PurchaseOrderStatus.ORDERED ? "発注済み" : "入荷済み",
                order.getOrderedAt(), order.getReceivedAt());
    }
}
