package com.example.demo;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
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
    private final Validator registrationValidator;

    /** 顧客保存、パスワード保護、入力検証の依存を受け取る。 */
    public CustomerRegistrationService(CustomerRepository customers, PasswordEncoder passwordEncoder, EntityManager entityManager, Validator registrationValidator) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
        this.registrationValidator = registrationValidator;
    }

    /** 入力と重複を検証し、ハッシュ化したパスワードで顧客を登録する。 */
    @Transactional
    public Map<String, Object> registerCustomer(RegistrationRequest registration) {
        var violations = registrationValidator.validate(registration);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
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
