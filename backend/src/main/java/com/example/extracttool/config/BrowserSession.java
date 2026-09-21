package com.example.extracttool.config;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/** Owns the single persistent browser session used for an operator-controlled Facebook login. */
@Component
public class BrowserSession {
    private final FacebookProperties properties;
    private Playwright playwright;
    private BrowserContext context;

    public BrowserSession(FacebookProperties properties) {
        this.properties = properties;
    }

    public synchronized BrowserContext context() {
        if (context != null) {
            return context;
        }
        try {
            Path profile = Path.of(properties.getProfileDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(profile);
            playwright = Playwright.create();
            context = playwright.chromium().launchPersistentContext(profile,
                    new BrowserType.LaunchPersistentContextOptions()
                            .setHeadless(properties.isHeadless())
                            .setViewportSize(1366, 900));
            context.setDefaultTimeout(properties.getNavigationTimeoutMs());
            context.setDefaultNavigationTimeout(properties.getNavigationTimeoutMs());
            return context;
        } catch (Exception exception) {
            close();
            throw new IllegalStateException("Cannot start the persistent Chromium profile. Close any other browser using the configured profile and try again.", exception);
        }
    }

    @PreDestroy
    public synchronized void close() {
        if (context != null) {
            context.close();
            context = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
}
