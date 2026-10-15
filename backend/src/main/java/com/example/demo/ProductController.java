package com.example.demo;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    /** 販売中の商品だけを登録順で返す。 */
    @GetMapping
    public Map<String, Object> getAvailableProducts() {
        var availableProducts = products.findAllByActiveTrueOrderById().stream()
                .map(ProductResponse::from).toList();
        return Map.of("status", "SUCCESS", "products", availableProducts);
    }
}
