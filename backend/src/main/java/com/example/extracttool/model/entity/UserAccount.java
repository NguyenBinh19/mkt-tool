package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    @Column(unique = true, length = 128) private String authToken;
    private Instant authTokenExpiresAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.USER;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AccountStatus status = AccountStatus.PENDING;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_feature_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "feature_key")
    private Set<String> permissions = new HashSet<>();

    public UserAccount(String name, String email, String passwordHash) { this.name = name; this.email = email; this.passwordHash = passwordHash; }
    public boolean canUse(String feature) { return role == Role.ADMIN || permissions.contains(feature); }
    public enum Role { ADMIN, USER }
    public enum AccountStatus { PENDING, APPROVED, REJECTED }
}
