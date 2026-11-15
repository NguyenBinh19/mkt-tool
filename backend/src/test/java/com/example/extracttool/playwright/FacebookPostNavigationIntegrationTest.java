package com.example.extracttool.playwright;

import com.example.extracttool.config.BrowserSession;
import com.example.extracttool.config.FacebookProperties;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.net.ServerSocket;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FacebookPostNavigationIntegrationTest {
    @TempDir Path temporaryDirectory;

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_FACEBOOK_NAVIGATION", matches = "1")
    void opensTheReportedPostInAnIsolatedChromiumProfile() throws IOException, InterruptedException {
        FacebookProperties properties = new FacebookProperties();
        properties.setHeadless(!"0".equals(System.getenv("TEST_FACEBOOK_HEADLESS")));
        properties.setProfileDirectory(System.getenv().getOrDefault("TEST_FACEBOOK_PROFILE_DIRECTORY",
                temporaryDirectory.resolve("chromium-profile").toString()));
        try (ServerSocket availablePort = new ServerSocket(0)) {
            properties.setDebugPort(availablePort.getLocalPort());
        }
        BrowserSession session = new BrowserSession(properties);
        try (BrowserSession.TaskPage taskPage = session.openTaskPage()) {
            String postUrl = System.getenv().getOrDefault("TEST_FACEBOOK_POST_URL",
                    "https://www.facebook.com/groups/734349847864531/permalink/1583918632907644/");
            new FacebookPostNavigator(properties).open(taskPage.page(), postUrl);
            System.out.println("Chromium final URL: " + taskPage.page().url());
            if ("1".equals(System.getenv("TEST_FACEBOOK_DIAGNOSTIC"))) {
                taskPage.page().waitForTimeout(3_000);
                System.out.println("Page title: " + taskPage.page().title());
                System.out.println("Main surface count: " + taskPage.page().locator("[role='main'], [role='feed']").count());
                System.out.println("Login input count: " + taskPage.page().locator("input[name='email']").count());
                taskPage.page().screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/facebook-navigation-diagnostic.png")));
            }
            assertTrue(taskPage.page().url().startsWith("https://www.facebook.com/"));
        } finally {
            session.close();
            // Windows releases Chromium's profile files shortly after process exit.
            Thread.sleep(1_500);
        }
    }
}
