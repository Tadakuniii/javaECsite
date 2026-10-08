package com.example.demo;

import java.security.Principal;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {
    private final CustomerRepository customers;
    private final CustomerRegistrationService registrationService;

    /** 顧客検索と登録サービスを受け取り、認証APIを構成する。 */
    public AuthenticationController(CustomerRepository customers, CustomerRegistrationService registrationService) {
        this.customers = customers;
        this.registrationService = registrationService;
    }

    /** フォーム送信に必要なCSRFトークンとヘッダー名を返す。 */
    @GetMapping("/api/csrf")
    public Map<String, Object> getCsrfToken(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    /** 未ログインでも取得できる初期案内を返す。 */
    @GetMapping("/api/webshop")
    public Map<String, Object> getWelcomeMessage() {
        return Map.of("status", "INITIAL", "message", "Please login");
    }

    /** 認証済みの顧客について表示名だけを返す。 */
    @GetMapping("/api/session")
    public Map<String, Object> getAuthenticatedCustomer(Principal principal) {
        Customer customer = customers.findById(principal.getName()).orElseThrow();
        return Map.of("status", "SUCCESS", "username", customer.getCustomerName());
    }

    /** フォームの各項目を受け取り、検証を含む会員登録を実行する。 */
    @PostMapping({"/api/register", "/api/registid", "/api/RegistUserID"})
    public Map<String, Object> registerCustomer(
            @RequestParam(defaultValue = "") String userid,
            @RequestParam(defaultValue = "") String password1,
            @RequestParam(defaultValue = "") String password2,
            @RequestParam(defaultValue = "") String username) {
        return registrationService.registerCustomer(new RegistrationRequest(userid, password1, password2, username));
    }
}
