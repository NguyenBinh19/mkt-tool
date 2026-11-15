package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "saved_leads", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "external_comment_id"}))
public class SavedLead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id") private Workspace workspace;
    @Column(name = "external_comment_id", nullable = false, length = 120) private String externalCommentId;
    @Column(length = 250) private String author;
    @Column(length = 2048) private String authorUrl;
    @Column(name = "post_url", length = 2048) private String postUrl;
    @Lob private String message;
    private String parentCommentId;
    private Instant commentCreatedAt;
    @Column(nullable = false) private Instant savedAt;

    public SavedLead(Workspace workspace, String externalCommentId) { this.workspace = workspace; this.externalCommentId = externalCommentId; }
    @PrePersist void created() { savedAt = Instant.now(); }
}
