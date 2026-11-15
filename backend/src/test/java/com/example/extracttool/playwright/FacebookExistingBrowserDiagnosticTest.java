package com.example.extracttool.playwright;

import com.example.extracttool.config.FacebookProperties;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

class FacebookExistingBrowserDiagnosticTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_FACEBOOK_EXISTING_BROWSER", matches = "1")
    void opensPostInTheCurrentProjectBrowser() {
        String url = System.getenv("TEST_FACEBOOK_POST_URL");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("TEST_FACEBOOK_POST_URL is required");
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().connectOverCDP("http://127.0.0.1:9222");
            Page page = browser.contexts().getFirst().newPage();
            AtomicReference<String> failedNavigation = new AtomicReference<>();
            page.onRequestFailed(request -> {
                if (request.isNavigationRequest()) failedNavigation.set(request.failure());
            });
            try {
                new FacebookPostNavigator(new FacebookProperties()).open(page, url);
                System.out.println("Current URL: " + page.url());
                System.out.println("Page title: " + page.title());
            } finally {
                System.out.println("Final URL: " + page.url());
                System.out.println("Navigation failure: " + failedNavigation.get());
                try {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(Path.of("target/facebook-current-browser-diagnostic.png"))
                            .setTimeout(5_000));
                } catch (RuntimeException ignored) {
                    System.out.println("Screenshot unavailable while the tab is loading");
                }
                page.close();
            }
        }
    }
}
