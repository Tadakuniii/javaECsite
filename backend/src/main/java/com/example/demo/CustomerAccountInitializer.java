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

    public CustomerAccountInitializer(CustomerRepository customers, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        importInitialCustomersIfDatabaseIsEmpty();
        hashLegacyCustomerPasswords();
    }

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
