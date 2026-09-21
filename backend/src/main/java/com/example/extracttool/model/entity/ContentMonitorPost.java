package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "content_monitor_posts", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "post_url"}))
public class ContentMonitorPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id") private Workspace workspace;
    @Column(name = "post_url", nullable = false, length = 2048) private String postUrl;
    @Column(length = 250) private String author;
    @Lob private String content;
    @Column(nullable = false) private boolean starred;
    @Column(nullable = false) private Instant searchedAt;
    public ContentMonitorPost(Workspace workspace, String postUrl) { this.workspace = workspace; this.postUrl = postUrl; }
    @PrePersist void created() { searchedAt = Instant.now(); }
}
