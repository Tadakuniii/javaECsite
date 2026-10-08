package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authentication;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureMockMvc
class AuthenticationIntegrationTests {
    @Autowired MockMvc api;
    @Autowired CustomerRepository customers;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CustomerAccountInitializer accountInitializer;

    /** 各テストを独立させるためH2の顧客データを消去する。 */
    @BeforeEach
    void clearTestCustomers() { customers.deleteAll(); }

    /** パスワードのハッシュ化と重複登録時の既存データ保護を検証する。 */
    @Test
    void registrationStoresHashedPasswordAndRejectsDuplicateId() throws Exception {
        registerCustomer();
        String storedPassword = customers.findById("test-user").orElseThrow().getCustomerPassword();
        assertThat(storedPassword).startsWith("{bcrypt}").isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", storedPassword)).isTrue();
        api.perform(post("/api/register").with(csrf()).param("userid", "test-user")
                .param("password1", "different123").param("password2", "different123"))
                .andExpect(jsonPath("$.status").value("EXISTING_ID"));
        assertThat(customers.count()).isEqualTo(1);
        assertThat(customers.findById("test-user").orElseThrow().getCustomerPassword()).isEqualTo(storedPassword);
    }

    /** ログイン応答、セッションID更新、状態保持、ログアウト後のアクセス拒否を検証する。 */
    @Test
    void loginPersistsSessionRotatesSessionIdAndLogoutRevokesAccess() throws Exception {
        registerCustomer();
        MvcResult csrfResult = api.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String originalSessionId = session.getId();
        api.perform(post("/api/login").session(session).with(csrf())
                .param("userid", "test-user").param("password", "password123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("テスト利用者"));
        assertThat(session.getId()).isNotEqualTo(originalSessionId);
        for (int request = 0; request < 2; request++) {
            api.perform(get("/api/session").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("テスト利用者"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
        api.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
        api.perform(get("/api/session")).andExpect(status().isUnauthorized());
    }

    /** 実際のCSRFトークンを使ってフォームを送信できることを検証する。 */
    @Test
    void csrfTokenEndpointWorksWithDefaultMaskedTokenProtection() throws Exception {
        MvcResult result = api.perform(get("/api/csrf")).andReturn();
        String json = result.getResponse().getContentAsString();
        String token = java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"")
                .matcher(json).results().findFirst().orElseThrow().group(1);
        api.perform(post("/api/register").session((MockHttpSession) result.getRequest().getSession())
                .header("X-CSRF-TOKEN", token).param("userid", "test-user")
                .param("password1", "password123").param("password2", "password123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    /** 引用符や改行を含む表示名も正しいJSONで返すことを検証する。 */
    @Test
    void loginResponseSafelySerializesDisplayName() throws Exception {
        String displayName = "利用者\"\\\n名前";
        customers.saveAndFlush(new Customer("quoted-user", passwordEncoder.encode("password123"), displayName));
        api.perform(post("/api/login").with(csrf()).param("userid", "quoted-user")
                .param("password", "password123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(displayName));
    }

    /** 必須項目が未送信でも400の入力エラーを返すことを検証する。 */
    @Test
    void missingRegistrationFieldsReturnValidationError() throws Exception {
        api.perform(post("/api/register").with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));
        assertThat(customers.count()).isZero();
    }

    /** 未ログインのセッション確認を拒否することを検証する。 */
    @Test
    void anonymousRequestsCannotReadCustomerSession() throws Exception {
        api.perform(get("/api/session")).andExpect(status().isUnauthorized());
    }

    /** 誤ったパスワードと旧デバッグIDで認証できないことを検証する。 */
    @Test
    void wrongPasswordAndRemovedDebugIdCannotAuthenticate() throws Exception {
        registerCustomer();
        api.perform(post("/api/login").with(csrf()).param("userid", "test-user").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        api.perform(post("/api/login").with(csrf()).param("userid", "LOgIN0000").param("password", "anything"))
                .andExpect(status().isUnauthorized());
    }

    /** 状態を変更する各APIがCSRFトークンなしの要求を拒否することを検証する。 */
    @Test
    void mutationRequestsRequireCsrfToken() throws Exception {
        api.perform(post("/api/register").param("userid", "test-user").param("password1", "password123")
                .param("password2", "password123")).andExpect(status().isForbidden());
        api.perform(post("/api/login").param("userid", "test-user").param("password", "password123"))
                .andExpect(status().isForbidden());
        api.perform(post("/api/logout")).andExpect(status().isForbidden());
        assertThat(customers.count()).isZero();
    }

    /** 空欄、不正なID、短いパスワードをサーバー側で拒否することを検証する。 */
    @Test
    void registrationRejectsBlankInvalidAndShortInputs() throws Exception {
        for (String userId : new String[]{"", "ab", "bad id"}) {
            api.perform(post("/api/register").with(csrf()).param("userid", userId)
                    .param("password1", "password123").param("password2", "password123"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));
        }
        api.perform(post("/api/register").with(csrf()).param("userid", "test-user")
                .param("password1", "short").param("password2", "short"))
                .andExpect(status().isBadRequest());
        assertThat(customers.count()).isZero();
    }

    /** 確認用パスワードの不一致とBCryptのバイト数制限を検証する。 */
    @Test
    void registrationRejectsMismatchedAndOversizedUtf8Passwords() throws Exception {
        api.perform(post("/api/register").with(csrf()).param("userid", "test-user")
                .param("password1", "password123").param("password2", "password456"))
                .andExpect(jsonPath("$.status").value("PASSWORD_MISMATCH"));
        String longPassword = "あ".repeat(25);
        api.perform(post("/api/register").with(csrf()).param("userid", "test-user")
                .param("password1", longPassword).param("password2", longPassword))
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"));
        assertThat(customers.count()).isZero();
    }

    /** 平文からの移行後も認証でき、繰り返し起動で再ハッシュ化しないことを検証する。 */
    @Test
    void migrationPreservesExistingPasswordAndDoesNotHashTwice() throws Exception {
        customers.saveAndFlush(new Customer("legacy-user", "oldpass", "既存利用者"));
        accountInitializer.run();
        String migratedPassword = customers.findById("legacy-user").orElseThrow().getCustomerPassword();
        assertThat(passwordEncoder.matches("oldpass", migratedPassword)).isTrue();
        accountInitializer.run();
        assertThat(customers.findById("legacy-user").orElseThrow().getCustomerPassword()).isEqualTo(migratedPassword);
        api.perform(post("/api/login").with(csrf()).param("userid", "legacy-user").param("password", "oldpass"))
                .andExpect(status().isOk());
    }

    /** 各認証テストで共通の顧客を登録する。 */
    private void registerCustomer() throws Exception {
        api.perform(post("/api/register").with(csrf()).param("userid", "test-user")
                .param("password1", "password123").param("password2", "password123")
                .param("username", "テスト利用者"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
