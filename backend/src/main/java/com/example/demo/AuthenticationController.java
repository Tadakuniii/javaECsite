package com.example.demo;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {
    private final CustomerRepository customers;
    private final CustomerRegistrationService registrationService;

    public AuthenticationController(CustomerRepository customers, CustomerRegistrationService registrationService) {
        this.customers = customers;
        this.registrationService = registrationService;
    }

    @GetMapping("/api/csrf")
    public Map<String, String> getCsrfToken(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/api/webshop")
    public Map<String, String> getWelcomeMessage() {
        return Map.of("status", "INITIAL", "message", "Please login");
    }

    @GetMapping("/api/session")
    public Map<String, String> getAuthenticatedCustomer(Principal principal) {
        Customer customer = customers.findById(principal.getName()).orElseThrow();
        return Map.of("status", "SUCCESS", "username", customer.getCustomerName());
    }

    @PostMapping({"/api/register", "/api/registid", "/api/RegistUserID"})
    public Map<String, String> registerCustomer(@Valid @ModelAttribute RegistrationRequest registration) {
        return registrationService.registerCustomer(registration);
    }
}
