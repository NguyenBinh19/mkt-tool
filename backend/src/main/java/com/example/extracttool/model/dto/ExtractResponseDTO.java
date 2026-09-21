package com.example.extracttool.model.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractResponseDTO {
    private String status;
    private String message;
    private List<CommentDTO> comments;
}
