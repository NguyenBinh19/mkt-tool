package com.example.extracttool.playwright;

import com.example.extracttool.config.FacebookProperties;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FacebookPostNavigatorTest {
    private static final String POST = "https://www.facebook.com/example/posts/pfbid123";
    private final FacebookPostNavigator navigator = new FacebookPostNavigator(new FacebookProperties());

    @Test
    void opensPostWhenNavigationCommitsBeforeDomIsReady() {
        Page page = mock(Page.class);
        Response response = mock(Response.class);
        when(response.status()).thenReturn(200);
        when(page.navigate(eq(POST), any(Page.NavigateOptions.class))).thenReturn(response);
        when(page.url()).thenReturn(POST);

        assertDoesNotThrow(() -> navigator.open(page, POST));
        verify(page).waitForLoadState(eq(LoadState.DOMCONTENTLOADED), any(Page.WaitForLoadStateOptions.class));
        verify(page, never()).navigate(eq("https://www.facebook.com/"), any(Page.NavigateOptions.class));
    }

    @Test
    void acceptsGroupPermalinkAfterNavigation() {
        String groupPost = "https://www.facebook.com/groups/734349847864531/permalink/1583918632907644/";
        Page page = mock(Page.class);
        Response response = mock(Response.class);
        when(response.status()).thenReturn(200);
        when(page.navigate(eq(groupPost), any(Page.NavigateOptions.class))).thenReturn(response);
        when(page.url()).thenReturn(groupPost);

        assertDoesNotThrow(() -> navigator.open(page, groupPost));
        verify(page, never()).navigate(eq("https://www.facebook.com/"), any(Page.NavigateOptions.class));
    }

    @Test
    void signalsBrowserRecoveryWhenPostNeverStartsLoading() {
        Page page = mock(Page.class);
        when(page.navigate(eq(POST), any(Page.NavigateOptions.class)))
                .thenThrow(new TimeoutError("first attempt timed out"));
        when(page.url()).thenReturn("about:blank");

        assertThrows(FacebookBrowserNavigationException.class, () -> navigator.open(page, POST));
        verify(page).navigate(eq(POST), any(Page.NavigateOptions.class));
        verify(page, never()).navigate(eq("https://www.facebook.com/"), any(Page.NavigateOptions.class));
    }

    @Test
    void reportsAnUnusableFacebookPageInsteadOfReturningZeroComments() {
        Page page = mock(Page.class);
        Response response = mock(Response.class);
        Locator surface = mock(Locator.class);
        when(response.status()).thenReturn(200);
        when(page.navigate(eq(POST), any(Page.NavigateOptions.class))).thenReturn(response);
        when(page.url()).thenReturn(POST);
        doThrow(new TimeoutError("dom was not ready"))
                .when(page).waitForLoadState(eq(LoadState.DOMCONTENTLOADED), any(Page.WaitForLoadStateOptions.class));
        when(page.locator(any(String.class))).thenReturn(surface);
        when(surface.count()).thenReturn(0);

        FacebookBrowserNavigationException error = assertThrows(FacebookBrowserNavigationException.class,
                () -> navigator.open(page, POST));
        assertTrue(error.getMessage().contains("did not show a usable page"));
    }
}
