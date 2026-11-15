package com.example.extracttool.service;

import com.example.extracttool.exception.AppException;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.repository.FeatureAccessRequestRepository;
import com.example.extracttool.repository.UserAccountRepository;
import com.example.extracttool.service.impl.AccessControlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {
    @Mock private UserAccountRepository users;
    @Mock private FeatureAccessRequestRepository requests;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AccessControlServiceImpl access;

    @BeforeEach
    void setUp() {
        access = new AccessControlServiceImpl(users, requests, passwordEncoder);
        ReflectionTestUtils.setField(access, "tokenTtlHours", 2);
    }

    @Test
    void registerHashesPasswordAndNormalizesEmail() {
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        access.register("Admin", " ADMIN@EXAMPLE.COM ", "a-safe-password");

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(passwordEncoder.matches("a-safe-password", saved.getValue().getPasswordHash())).isTrue();
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("a-safe-password");
    }

    @Test
    void loginGeneratesExpiringOpaqueToken() {
        UserAccount user = new UserAccount("Admin", "admin@example.com", passwordEncoder.encode("a-safe-password"));
        user.setStatus(UserAccount.AccountStatus.APPROVED);
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        UserAccount result = access.login("admin@example.com", "a-safe-password");

        assertThat(result.getAuthToken()).hasSize(43).doesNotContain("=");
        assertThat(result.getAuthTokenExpiresAt()).isAfter(before.plusSeconds(7_100));
        assertThat(result.getAuthTokenExpiresAt()).isBefore(before.plusSeconds(7_300));
    }

    @Test
    void requireRejectsExpiredToken() {
        UserAccount user = new UserAccount("Admin", "admin@example.com", "hash");
        user.setStatus(UserAccount.AccountStatus.APPROVED);
        user.setAuthToken("expired-token");
        user.setAuthTokenExpiresAt(Instant.now().minusSeconds(1));
        when(users.findByAuthToken("expired-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> access.require("expired-token"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Phiên đăng nhập");
        verify(users).findByAuthToken(eq("expired-token"));
    }
}
