package com.example.extracttool.config;

import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.repository.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
public class DatabaseSeeder {
    @Bean
    CommandLineRunner seedConfiguredAdmin(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.admin.email:}") String email,
            @Value("${bootstrap.admin.password:}") String password) {
        return args -> {
            if (email.isBlank() || password.isBlank()) {
                log.warn("No bootstrap administrator was configured. Set BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD before the first production start.");
                return;
            }
            if (password.length() < 12) {
                throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters.");
            }
            users.findByEmailIgnoreCase(email).orElseGet(() -> {
                UserAccount admin = new UserAccount("Administrator", email.trim().toLowerCase(), passwordEncoder.encode(password));
                admin.setRole(UserAccount.Role.ADMIN);
                admin.setStatus(UserAccount.AccountStatus.APPROVED);
                return users.save(admin);
            });
        };
    }

    @Bean
    @Profile("local")
    CommandLineRunner seedLocalDemoUser(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        return args -> users.findByEmailIgnoreCase("user@leadflow.local").orElseGet(() -> {
            UserAccount user = new UserAccount("Demo User", "user@leadflow.local", passwordEncoder.encode("user123"));
            user.setStatus(UserAccount.AccountStatus.APPROVED);
            user.getPermissions().add("Extract comment");
            user.getPermissions().add("Lead scoring");
            user.getPermissions().add("Content monitor");
            return users.save(user);
        });
    }
}
