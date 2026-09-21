package com.example.extracttool.config;

import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {
    private final UserAccountRepository users;

    @Bean
    CommandLineRunner seedDefaultAdmin() {
        return args -> users.findByEmailIgnoreCase("admin@leadflow.local").orElseGet(() -> {
            UserAccount admin = new UserAccount("Admin", "admin@leadflow.local", new BCryptPasswordEncoder().encode("admin123"));
            admin.setRole(UserAccount.Role.ADMIN);
            admin.setStatus(UserAccount.AccountStatus.APPROVED);
            return users.save(admin);
        });
    }
}
