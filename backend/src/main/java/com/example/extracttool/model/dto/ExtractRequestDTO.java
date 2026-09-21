package com.example.extracttool.model.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractRequestDTO {
    @NotBlank
    private String postUrl;
    @Min(1)
    private Integer maxComments;
    private boolean includeReplies;
}
