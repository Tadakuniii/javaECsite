package com.example.demo;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRegistrationService {
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public CustomerRegistrationService(CustomerRepository customers, PasswordEncoder passwordEncoder, EntityManager entityManager) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional
    public Map<String, String> registerCustomer(RegistrationRequest registration) {
        if (!registration.password1().equals(registration.password2())) {
            return Map.of("status", "PASSWORD_MISMATCH", "message", "異なるパスワードが入力されました");
        }
        if (registration.password1().getBytes(StandardCharsets.UTF_8).length > 72) {
            return Map.of("status", "VALIDATION_ERROR", "message", "パスワードが長すぎます。文字数を減らしてください");
        }
        if (customers.existsById(registration.userid())) {
            return Map.of("status", "EXISTING_ID", "message", "入力されたユーザIDは登録済です");
        }
        String displayName = registration.username() == null || registration.username().isBlank()
                ? "新規ユーザー" : registration.username().strip();
        entityManager.persist(new Customer(registration.userid(),
                passwordEncoder.encode(registration.password1()), displayName));
        customers.flush();
        return Map.of("status", "SUCCESS", "message", "ユーザIDが登録されました");
    }
}
