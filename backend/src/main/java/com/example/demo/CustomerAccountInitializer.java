package com.example.demo;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomerAccountInitializer implements CommandLineRunner {
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;

    /** 初期顧客の保存と既存パスワードの移行に必要な依存を受け取る。 */
    public CustomerAccountInitializer(CustomerRepository customers, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
    }

    /** 起動時に初期顧客を取り込み、既存の平文パスワードを移行する。 */
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        importInitialCustomersIfDatabaseIsEmpty();
        hashLegacyCustomerPasswords();
    }

    /** DBが空の場合だけ、学習用ファイルから顧客をハッシュ化して登録する。 */
    private void importInitialCustomersIfDatabaseIsEmpty() throws java.io.IOException {
        Path initialCustomersFile = Path.of("src/main/java/com/example/demo/useridInfo.txt");
        if (customers.count() != 0 || !Files.isReadable(initialCustomersFile)) return;

        for (String line : Files.readAllLines(initialCustomersFile)) {
            if (line.isBlank()) continue;
            String[] fields = line.split(",");
            if (fields.length >= 3) {
                customers.save(new Customer(fields[0].strip(),
                        passwordEncoder.encode(fields[1].strip()), fields[2].strip()));
            }
        }
    }

    /** 既存アカウントのパスワードを一度だけハッシュ化する。 */
    private void hashLegacyCustomerPasswords() {
        // Preserve existing credentials and avoid hashing migrated passwords again.
        for (Customer customer : customers.findAll()) {
            if (!customer.getCustomerPassword().startsWith("{bcrypt}")) {
                customer.setCustomerPassword(passwordEncoder.encode(customer.getCustomerPassword()));
                customers.save(customer);
            }
        }
    }
}
