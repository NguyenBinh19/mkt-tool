package com.example.extracttool.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "facebook")
public class FacebookProperties {
    private String profileDirectory = "playwright-profile";
    private boolean headless = false;
    private int navigationTimeoutMs = 45_000;
    private int maxCommentsLimit = 1_000;
    private int debugPort = 9222;
    @Min(1)
    private int maxConcurrentTabs = 1;
}
