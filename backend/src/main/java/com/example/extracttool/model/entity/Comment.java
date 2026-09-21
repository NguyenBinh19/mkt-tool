package com.example.extracttool.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comments", uniqueConstraints = @UniqueConstraint(columnNames = {"postUrl", "fbId"}))
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fbId;
    private String postUrl;
    private String author;
    @Column(name = "author_url", length = 2048)
    private String authorUrl;

    @Lob
    private String message;

    private Instant createdAt;
    private String parentFbId;
}
