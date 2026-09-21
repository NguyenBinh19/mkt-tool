package com.example.extracttool.config;

import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedDefaultAdmin() {
        return args -> {
            users.findByEmailIgnoreCase("admin@leadflow.local").orElseGet(() -> {
                UserAccount admin = new UserAccount("Admin", "admin@leadflow.local", passwordEncoder.encode("admin123"));
                admin.setRole(UserAccount.Role.ADMIN);
                admin.setStatus(UserAccount.AccountStatus.APPROVED);
                return users.save(admin);
            });
            users.findByEmailIgnoreCase("user@leadflow.local").orElseGet(() -> {
                UserAccount user = new UserAccount("Demo User", "user@leadflow.local", passwordEncoder.encode("user123"));
                user.setStatus(UserAccount.AccountStatus.APPROVED);
                user.getPermissions().add("Extract comment");
                user.getPermissions().add("Lead scoring");
                user.getPermissions().add("Content monitor");
                return users.save(user);
            });
        };
    }
}
