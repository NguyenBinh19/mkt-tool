package com.example.extracttool.service;

import com.example.extracttool.config.BrowserSession;
import com.example.extracttool.config.FacebookProperties;
import com.example.extracttool.model.dto.ExtractRequestDTO;
import com.example.extracttool.playwright.FacebookAuthenticationRequiredException;
import com.example.extracttool.playwright.FacebookBrowserNavigationException;
import com.example.extracttool.playwright.PlaywrightExtractor;
import com.example.extracttool.repository.CommentRepository;
import com.example.extracttool.service.impl.FacebookCommentServiceImpl;
import com.example.extracttool.validation.FacebookPostUrlValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FacebookCommentServiceImplTest {
    @Test
    void restartsStalledBrowserAndRetriesExtractionOnce() {
        PlaywrightExtractor extractor = mock(PlaywrightExtractor.class);
        BrowserSession browserSession = mock(BrowserSession.class);
        FacebookCommentServiceImpl service = new FacebookCommentServiceImpl(extractor,
                mock(CommentRepository.class), new FacebookPostUrlValidator(),
                new FacebookProperties(), browserSession);
        ExtractRequestDTO request = new ExtractRequestDTO("https://www.facebook.com/example/posts/pfbid123", 1, false);
        when(extractor.extract(anyString(), eq(1), eq(false)))
                .thenThrow(new FacebookBrowserNavigationException("Navigation stalled", null))
                .thenReturn(java.util.List.of());

        assertEquals("ok", service.extractComments(request).getStatus());
        verify(browserSession).retry();
        verify(extractor, times(2)).extract(anyString(), eq(1), eq(false));
    }

    @Test
    void reportsErrorAfterOneBrowserRecoveryAttempt() {
        PlaywrightExtractor extractor = mock(PlaywrightExtractor.class);
        BrowserSession browserSession = mock(BrowserSession.class);
        FacebookCommentServiceImpl service = new FacebookCommentServiceImpl(extractor,
                mock(CommentRepository.class), new FacebookPostUrlValidator(),
                new FacebookProperties(), browserSession);
        ExtractRequestDTO request = new ExtractRequestDTO("https://www.facebook.com/example/posts/pfbid123", 1, false);
        when(extractor.extract(anyString(), eq(1), eq(false)))
                .thenThrow(new FacebookBrowserNavigationException("Navigation stalled", null));

        assertEquals("error", service.extractComments(request).getStatus());
        verify(browserSession).retry();
        verify(extractor, times(2)).extract(anyString(), eq(1), eq(false));
    }

    @Test
    void restartsStalledBrowserAndRetriesSearchOnce() {
        PlaywrightExtractor extractor = mock(PlaywrightExtractor.class);
        BrowserSession browserSession = mock(BrowserSession.class);
        FacebookCommentServiceImpl service = new FacebookCommentServiceImpl(extractor,
                mock(CommentRepository.class), new FacebookPostUrlValidator(),
                new FacebookProperties(), browserSession);
        when(extractor.searchPosts("evo75", 10, true))
                .thenThrow(new FacebookBrowserNavigationException("Home stalled", null))
                .thenReturn(java.util.List.of());

        assertEquals(0, service.searchPosts("evo75", 10, true).size());
        verify(browserSession).retry();
        verify(extractor, times(2)).searchPosts("evo75", 10, true);
    }

    @Test
    void distinguishesBrowserNavigationFailureFromFacebookAuthentication() {
        PlaywrightExtractor extractor = mock(PlaywrightExtractor.class);
        FacebookCommentServiceImpl service = new FacebookCommentServiceImpl(extractor,
                mock(CommentRepository.class), new FacebookPostUrlValidator(),
                new FacebookProperties(), mock(BrowserSession.class));
        ExtractRequestDTO request = new ExtractRequestDTO("https://www.facebook.com/example/posts/pfbid123", 1, false);
        when(extractor.extract(anyString(), eq(1), eq(false)))
                .thenThrow(new IllegalStateException("Chromium navigation timed out"))
                .thenThrow(new FacebookAuthenticationRequiredException("Facebook verification required"));

        assertEquals("error", service.extractComments(request).getStatus());
        assertEquals("authentication_required", service.extractComments(request).getStatus());
    }
}
