package com.example.extracttool.service.impl;

import com.example.extracttool.model.entity.FeatureAccessRequest;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.exception.AppException;
import com.example.extracttool.exception.ErrorCode;
import com.example.extracttool.repository.FeatureAccessRequestRepository;
import com.example.extracttool.repository.UserAccountRepository;
import com.example.extracttool.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements AccessControlService {
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private static final Set<String> AVAILABLE_FEATURES = Set.of("Extract comment", "Lead scoring", "Content monitor");
    private final UserAccountRepository users;
    private final FeatureAccessRequestRepository requests;
    private final PasswordEncoder passwordEncoder;
    @Value("${security.token-ttl-hours:24}") private int tokenTtlHours;

    @Override public UserAccount register(String name, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(normalizedEmail).isPresent()) throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        return users.save(new UserAccount(name.trim(), normalizedEmail, passwordEncoder.encode(password)));
    }
    @Override public UserAccount login(String email, String password) {
        UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(this::unauthorized);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw unauthorized();
        if (user.getStatus() != UserAccount.AccountStatus.APPROVED) throw new AppException(ErrorCode.ACCOUNT_NOT_APPROVED);
        user.setAuthToken(newToken());
        user.setAuthTokenExpiresAt(Instant.now().plus(Math.max(1, tokenTtlHours), ChronoUnit.HOURS));
        return users.save(user);
    }
    @Override public UserAccount require(String token) {
        UserAccount user = users.findByAuthToken(token).orElseThrow(() -> new AppException(ErrorCode.INVALID_SESSION));
        if (user.getStatus() != UserAccount.AccountStatus.APPROVED || user.getAuthTokenExpiresAt() == null || user.getAuthTokenExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.INVALID_SESSION);
        }
        return user;
    }
    @Override public void requireAdmin(UserAccount user) { if (user.getRole() != UserAccount.Role.ADMIN) throw new AppException(ErrorCode.ACCESS_DENIED); }
    @Override public void requireFeature(UserAccount user, String feature) { if (!user.canUse(feature)) throw new AppException(ErrorCode.ACCESS_DENIED); }
    @Override public FeatureAccessRequest requestFeature(UserAccount user, String feature) {
        if (!AVAILABLE_FEATURES.contains(feature)) throw new AppException(ErrorCode.FEATURE_NOT_FOUND);
        if (user.getRole() == UserAccount.Role.ADMIN || user.canUse(feature)) throw new AppException(ErrorCode.FEATURE_ALREADY_GRANTED);
        if (requests.existsByUserIdAndFeatureKeyAndStatus(user.getId(), feature, FeatureAccessRequest.RequestStatus.PENDING)) throw new AppException(ErrorCode.FEATURE_REQUEST_PENDING);
        return requests.save(new FeatureAccessRequest(user, feature));
    }
    @Override public List<FeatureAccessRequest> requestsFor(UserAccount user) { return requests.findByUserIdOrderByCreatedAtDesc(user.getId()); }
    @Override public List<FeatureAccessRequest> pendingRequests() { return requests.findByStatus(FeatureAccessRequest.RequestStatus.PENDING); }
    @Override @Transactional public FeatureAccessRequest decideRequest(Long id, boolean approve, UserAccount admin) {
        requireAdmin(admin);
        FeatureAccessRequest request = requests.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCESS_REQUEST_NOT_FOUND));
        request.setStatus(approve ? FeatureAccessRequest.RequestStatus.APPROVED : FeatureAccessRequest.RequestStatus.REJECTED);
        if (approve) {
            request.getUser().getPermissions().add(request.getFeatureKey());
            users.save(request.getUser());
        }
        return requests.save(request);
    }
    @Override public List<UserAccount> users(UserAccount admin) { requireAdmin(admin); return users.findAll(); }
    @Override public UserAccount decideUser(Long id, boolean approve, UserAccount admin) {
        requireAdmin(admin);
        UserAccount user = users.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(approve ? UserAccount.AccountStatus.APPROVED : UserAccount.AccountStatus.REJECTED);
        return users.save(user);
    }
    private AppException unauthorized() { return new AppException(ErrorCode.INVALID_CREDENTIALS); }
    private String newToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
