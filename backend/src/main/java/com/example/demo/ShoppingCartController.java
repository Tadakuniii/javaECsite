package com.example.demo;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class ShoppingCartController {
    private final ShoppingCartService shoppingCart;

    public ShoppingCartController(ShoppingCartService shoppingCart) {
        this.shoppingCart = shoppingCart;
    }

    @GetMapping
    public ShoppingCartResponse getCart(Principal principal) {
        return shoppingCart.getCart(principal.getName());
    }

    @PostMapping("/items")
    public ShoppingCartResponse addItem(Principal principal, @Valid @RequestBody AddCartItemRequest request) {
        return shoppingCart.addItem(principal.getName(), request);
    }

    @PutMapping("/items/{itemId}")
    public ShoppingCartResponse updateItem(Principal principal, @PathVariable long itemId,
                                           @Valid @RequestBody CartItemQuantityRequest request) {
        return shoppingCart.updateItem(principal.getName(), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public ShoppingCartResponse removeItem(Principal principal, @PathVariable long itemId) {
        return shoppingCart.removeItem(principal.getName(), itemId);
    }
}
