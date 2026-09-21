package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "content_monitor_history")
public class ContentMonitorHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workspace_id") private Workspace workspace;
    @Column(nullable = false, length = 250) private String keyword;
    @Column(nullable = false) private int resultCount;
    @Column(nullable = false) private Instant createdAt;
    public ContentMonitorHistory(Workspace workspace, String keyword, int resultCount) { this.workspace = workspace; this.keyword = keyword; this.resultCount = resultCount; }
    @PrePersist void created() { createdAt = Instant.now(); }
}
