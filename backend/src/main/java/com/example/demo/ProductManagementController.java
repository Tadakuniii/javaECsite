package com.example.demo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class ProductManagementController {
    private final ProductRepository products;
    private final ProductManagementService productManagement;

    public ProductManagementController(ProductRepository products, ProductManagementService productManagement) {
        this.products = products;
        this.productManagement = productManagement;
    }

    @GetMapping
    public List<ManagedProductResponse> listProducts() {
        return products.findAllByOrderById().stream().map(ManagedProductResponse::from).toList();
    }

    @PostMapping
    public ManagedProductResponse createProduct(@Valid @RequestBody ProductManagementRequest request) {
        return productManagement.create(request);
    }

    @PutMapping("/{productId}")
    public ManagedProductResponse updateProduct(@PathVariable long productId,
                                                @Valid @RequestBody ProductManagementRequest request) {
        return productManagement.update(productId, request);
    }
}
