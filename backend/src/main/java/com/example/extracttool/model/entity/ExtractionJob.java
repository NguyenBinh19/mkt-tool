package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extraction_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ExtractionJob {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false) private Long ownerId;
    @Column(nullable = false) private Long workspaceId;
    @Column(nullable = false, length = 2048) private String postUrl;
    @Column(nullable = false) private int maxComments;
    @Column(nullable = false) private boolean includeReplies;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    @Lob @Column(columnDefinition = "LONGTEXT") private String resultJson;
    @Column(length = 2000) private String errorMessage;
    @Column(nullable = false) private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED }

    public ExtractionJob(Long ownerId, Long workspaceId, String postUrl, int maxComments, boolean includeReplies) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.workspaceId = workspaceId;
        this.postUrl = postUrl;
        this.maxComments = maxComments;
        this.includeReplies = includeReplies;
        this.status = Status.QUEUED;
        this.createdAt = Instant.now();
    }
}
