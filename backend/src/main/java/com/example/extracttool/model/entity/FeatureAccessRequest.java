package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity @Table(name = "feature_access_requests")
public class FeatureAccessRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private UserAccount user;
    @Column(nullable = false) private String featureKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RequestStatus status = RequestStatus.PENDING;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    public FeatureAccessRequest(UserAccount user, String featureKey) { this.user = user; this.featureKey = featureKey; }
    public enum RequestStatus { PENDING, APPROVED, REJECTED }
}
