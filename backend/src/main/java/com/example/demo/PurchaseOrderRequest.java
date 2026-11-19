package com.example.demo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PurchaseOrderRequest(
        @NotNull Long productId,
        @NotBlank @Size(max = 200) String supplierName,
        @NotNull LocalDate expectedDeliveryDate,
        @Min(0) int unitCost,
        @Min(1) int quantity) {
}
