package com.example.extracttool.model.dto;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private String id; // facebook comment id
    private String author;
    private String message;
    private Instant createdAt;
    private String parentId;
}
