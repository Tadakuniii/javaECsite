package com.example.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByActiveTrueOrderById();
    List<Product> findAllByOrderById();
    boolean existsByProductCode(String productCode);
}
