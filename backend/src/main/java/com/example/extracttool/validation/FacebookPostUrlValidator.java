package com.example.extracttool.validation;

import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class FacebookPostUrlValidator {
    public String validate(String value) {
        if (value == null || value.isBlank()) {
            return "postUrl is required";
        }
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) {
                return "URL must be an HTTPS Facebook post URL";
            }
            String normalizedHost = host.toLowerCase();
            boolean facebook = normalizedHost.equals("facebook.com") || normalizedHost.endsWith(".facebook.com")
                    || normalizedHost.equals("fb.watch");
            return facebook ? null : "URL must point to facebook.com or fb.watch";
        } catch (IllegalArgumentException exception) {
            return "Invalid URL";
        }
    }
}
