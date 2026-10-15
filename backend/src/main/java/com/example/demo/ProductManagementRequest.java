package com.example.demo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductManagementRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String category,
        @NotBlank @Size(max = 2000) String description,
        @Min(0) int price,
        @Min(0) int stockQuantity,
        @Size(max = 500) String imageUrl,
        boolean active) {
}
