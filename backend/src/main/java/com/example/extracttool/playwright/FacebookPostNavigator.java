package com.example.extracttool.playwright;

import com.example.extracttool.config.FacebookProperties;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Opens a post without waiting for Facebook's entire JavaScript application to settle. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FacebookPostNavigator {
    private static final String FACEBOOK_UI = "[role='main'], [role='feed'], [role='dialog'], input[name='email']";
    private final FacebookProperties properties;

    public void open(Page page, String postUrl) {
        page.onRequestFailed(request -> {
            if (request.isNavigationRequest() && request.url().startsWith("https://www.facebook.com/")) {
                log.warn("Facebook navigation request failed: {} ({})", request.url(), request.failure());
            }
        });

        try {
            navigateToPost(page, postUrl);
        } catch (TimeoutError firstTimeout) {
            if (!isPostNavigation(page.url())) {
                throw new FacebookBrowserNavigationException("The Facebook tab did not start loading within "
                        + commitTimeoutMs() + " ms. The browser session may be stalled.", firstTimeout);
            }
        }

        if (!isFacebookPage(page.url())) {
            throw new FacebookBrowserNavigationException("The Facebook tab did not open the post. Current page: "
                    + page.url(), null);
        }

        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(contentTimeoutMs()));
        } catch (TimeoutError timeout) {
            // Facebook may keep loading scripts after its visible post is usable.
            // Only continue when an actual Facebook surface is already present.
            if (page.locator(FACEBOOK_UI).count() == 0) {
                throw new FacebookBrowserNavigationException("Facebook opened the URL but did not show a usable page within "
                        + contentTimeoutMs() + " ms. Final URL: " + page.url(), timeout);
            }
        }

        if (!isPostNavigation(page.url()) && !isAuthenticationPage(page.url())) {
            throw new IllegalStateException("Facebook redirected away from the requested post to " + page.url());
        }
    }

    private void navigateToPost(Page page, String postUrl) {
        Response response = page.navigate(postUrl, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.COMMIT).setTimeout(commitTimeoutMs()));
        if (response != null && response.status() >= 400) {
            throw new IllegalStateException("Facebook returned HTTP " + response.status() + " for this post.");
        }
    }

    private boolean isFacebookPage(String url) {
        return url != null && (url.startsWith("https://www.facebook.com/")
                || url.startsWith("https://facebook.com/") || url.startsWith("https://m.facebook.com/"));
    }

    private boolean isPostNavigation(String url) {
        return isFacebookPage(url) && (url.contains("/posts/") || url.contains("/permalink.php")
                || url.contains("story_fbid=") || url.contains("multi_permalinks=") || url.contains("/photo/")
                || url.contains("/photos/") || url.contains("/photo.php") || url.contains("fbid=")
                || url.contains("/watch/") || url.contains("/videos/") || url.contains("/share/p/")
                || url.contains("/reel/") || url.matches(".*?/groups/[^/]+/permalink/[^/]+/?(?:\\?.*)?"));
    }

    private boolean isAuthenticationPage(String url) {
        return isFacebookPage(url) && (url.contains("/login") || url.contains("/checkpoint"));
    }

    private int commitTimeoutMs() { return Math.min(20_000, properties.getNavigationTimeoutMs()); }
    private int contentTimeoutMs() { return Math.min(15_000, properties.getNavigationTimeoutMs()); }
}
