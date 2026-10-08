package com.example.demo;

import java.util.Map;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuthenticationSecurityConfiguration {
    /** BCryptを標準とするパスワードエンコーダーを提供する。 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** DBの顧客情報をSpring Securityの認証情報として読み込む。 */
    @Bean
    UserDetailsService customerUserDetailsService(CustomerRepository customers) {
        return userId -> customers.findById(userId)
                .map(customer -> User.withUsername(customer.getCustomerId())
                        .password(customer.getCustomerPassword()).roles("CUSTOMER").build())
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));
    }

    /** 公開API、セッション認証、CSRF保護、JSONでの認証結果を設定する。 */
    @Bean
    SecurityFilterChain authenticationSecurityFilterChain(HttpSecurity http, CustomerRepository customers, JsonMapper jsonMapper) throws Exception {
        http.authorizeHttpRequests(access -> access
                .requestMatchers("/api/csrf", "/api/login", "/api/register",
                        "/api/registid", "/api/RegistUserID", "/api/webshop", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(login -> login.loginProcessingUrl("/api/login").usernameParameter("userid")
                .successHandler((request, response, authentication) -> {
                    Customer customer = customers.findById(authentication.getName()).orElseThrow();
                    writeJson(response, 200, jsonMapper.writeValueAsString(
                            Map.of("status", "SUCCESS", "username", customer.getCustomerName())));
                })
                .failureHandler((request, response, exception) -> writeJson(response, 401,
                        "{\"status\":\"RELOGIN\",\"message\":\"ユーザIDまたはパスワードが間違っています\"}")))
            .logout(logout -> logout.logoutUrl("/api/logout").deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                        writeJson(response, 200, "{\"status\":\"SUCCESS\"}")))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> writeJson(response, 401,
                        "{\"status\":\"UNAUTHENTICATED\",\"message\":\"ログインしてください\"}"))
                .accessDeniedHandler((request, response, exception) -> writeJson(response, 403,
                        "{\"status\":\"FORBIDDEN\",\"message\":\"操作を確認できませんでした。画面を再読み込みしてください\"}")));
        // Keep Spring Security's session fixation protection and CSRF protection enabled.
        return http.build();
    }

    /** 認証フィルターの応答にHTTPステータスとUTF-8のJSONを設定する。 */
    private static void writeJson(HttpServletResponse response, int status, String json) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }
}
