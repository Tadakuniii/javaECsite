package com.example.demo;

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
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService customerUserDetailsService(CustomerRepository customers) {
        return userId -> customers.findById(userId)
                .map(customer -> User.withUsername(customer.getCustomerId())
                        .password(customer.getCustomerPassword()).roles("CUSTOMER").build())
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));
    }

    @Bean
    SecurityFilterChain authenticationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(access -> access
                .requestMatchers("/api/csrf", "/api/login", "/api/register",
                        "/api/registid", "/api/RegistUserID", "/api/webshop", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(login -> login.loginProcessingUrl("/api/login").usernameParameter("userid")
                .successHandler((request, response, authentication) ->
                        writeJson(response, 200, "{\"status\":\"SUCCESS\"}"))
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

    private static void writeJson(HttpServletResponse response, int status, String json) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }
}
