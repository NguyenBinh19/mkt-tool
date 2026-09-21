package com.example.extracttool.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FacebookSearchRequest {
    @NotBlank
    @Size(max = 160)
    private String keyword;

    @Min(1)
    @Max(50)
    private Integer limit = 20;

    private boolean recentPosts;
}
