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

    @Lob
    private String message;

    private Instant createdAt;
    private String parentFbId;
}
