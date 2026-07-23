package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    public DataInitializer(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (customerRepository.count() == 0) {
            System.out.println("No users found in MySQL. Migrating from useridInfo.txt...");
            File file = new File("src/main/java/com/example/demo/useridInfo.txt");
            if (file.exists() && file.isFile() && file.canRead()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            String id = parts[0].trim();
                            String password = parts[1].trim();
                            String name = parts[2].trim();
                            customerRepository.save(new Customer(id, password, name));
                            System.out.println("Migrated user: " + name);
                        }
                    }
                }
            } else {
                System.out.println("Migration source file not found or not readable: " + file.getAbsolutePath());
            }
        } else {
            System.out.println("Database already contains users. Skipping migration.");
        }
    }
}
