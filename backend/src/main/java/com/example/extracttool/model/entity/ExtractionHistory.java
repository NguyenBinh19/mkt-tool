package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "extraction_history")
public class ExtractionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id") private Workspace workspace;
    @Column(nullable = false, length = 2048) private String postUrl;
    @Column(nullable = false) private int commentCount;
    @Lob @Column(name = "result_json", columnDefinition = "LONGTEXT") private String resultJson;
    @Column(nullable = false) private Instant createdAt;
    public ExtractionHistory(Workspace workspace, String postUrl, int commentCount) { this.workspace = workspace; this.postUrl = postUrl; this.commentCount = commentCount; }
    @PrePersist void created() { createdAt = Instant.now(); }
}
