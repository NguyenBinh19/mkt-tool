package com.example.extracttool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "facebook")
public class FacebookProperties {
    private String profileDirectory = "playwright-profile";
    private boolean headless = false;
    private int navigationTimeoutMs = 45_000;
    private int maxCommentsLimit = 1_000;

    public String getProfileDirectory() { return profileDirectory; }
    public void setProfileDirectory(String profileDirectory) { this.profileDirectory = profileDirectory; }
    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { this.headless = headless; }
    public int getNavigationTimeoutMs() { return navigationTimeoutMs; }
    public void setNavigationTimeoutMs(int navigationTimeoutMs) { this.navigationTimeoutMs = navigationTimeoutMs; }
    public int getMaxCommentsLimit() { return maxCommentsLimit; }
    public void setMaxCommentsLimit(int maxCommentsLimit) { this.maxCommentsLimit = maxCommentsLimit; }
}
