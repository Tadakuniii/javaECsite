package com.example.demo;

import jakarta.validation.constraints.Min;

public record CartItemQuantityRequest(@Min(1) int quantity) {
}
