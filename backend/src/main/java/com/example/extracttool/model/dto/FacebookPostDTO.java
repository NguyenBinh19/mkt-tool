package com.example.extracttool.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FacebookPostDTO {
    String url;
    String author;
    String text;
    String publishedAt;
}
