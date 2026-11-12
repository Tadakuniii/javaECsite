package com.example.demo;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderHistoryResponse> getOrderHistory(Principal principal) {
        return orderService.getOrderHistory(principal.getName());
    }

    @PostMapping
    public OrderHistoryResponse checkout(Principal principal) {
        return orderService.checkout(principal.getName());
    }
}
