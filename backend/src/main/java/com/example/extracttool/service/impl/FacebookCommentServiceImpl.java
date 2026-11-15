package com.example.extracttool.service.impl;

import com.example.extracttool.config.BrowserSession;
import com.example.extracttool.config.FacebookProperties;
import com.example.extracttool.model.dto.CommentDTO;
import com.example.extracttool.model.dto.ExtractRequestDTO;
import com.example.extracttool.model.dto.ExtractResponseDTO;
import com.example.extracttool.model.entity.Comment;
import com.example.extracttool.playwright.PlaywrightExtractor;
import com.example.extracttool.playwright.FacebookAuthenticationRequiredException;
import com.example.extracttool.playwright.FacebookBrowserNavigationException;
import com.example.extracttool.repository.CommentRepository;
import com.example.extracttool.service.FacebookCommentService;
import com.example.extracttool.validation.FacebookPostUrlValidator;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
public class FacebookCommentServiceImpl implements FacebookCommentService {
    private final PlaywrightExtractor extractor;
    private final CommentRepository commentRepository;
    private final FacebookPostUrlValidator urlValidator;
    private final FacebookProperties properties;
    private final BrowserSession browserSession;

    public FacebookCommentServiceImpl(PlaywrightExtractor extractor, CommentRepository commentRepository,
                                      FacebookPostUrlValidator urlValidator, FacebookProperties properties,
                                      BrowserSession browserSession) {
        this.extractor = extractor;
        this.commentRepository = commentRepository;
        this.urlValidator = urlValidator;
        this.properties = properties;
        this.browserSession = browserSession;
    }

    @Override
    public ExtractResponseDTO extractComments(ExtractRequestDTO request) {
        String error = urlValidator.validate(request.getPostUrl());
        if (error != null) return response("error", error, List.of());

        int max = request.getMaxComments() == null ? 200 : request.getMaxComments();
        if (max < 1 || max > properties.getMaxCommentsLimit()) {
            return response("error", "maxComments must be between 1 and " + properties.getMaxCommentsLimit(), List.of());
        }

        try {
            List<CommentDTO> comments = withBrowserRecovery(() ->
                    extractor.extract(request.getPostUrl().trim(), max, request.isIncludeReplies()));
            browserSession.markAuthenticated();
            persistNewComments(request.getPostUrl().trim(), comments);
            return response("ok", "Extracted " + comments.size() + " comment(s).", comments);
        } catch (FacebookAuthenticationRequiredException exception) {
            browserSession.markAuthenticationRequired();
            return response("authentication_required", exception.getMessage(), List.of());
        } catch (Exception exception) {
            return response("error", "Could not extract comments: " + exception.getMessage(), List.of());
        }
    }

    @Override
    public void openBrowserForLogin() {
        browserSession.openLoginPage();
    }

    @Override
    public List<com.example.extracttool.model.dto.FacebookPostDTO> searchPosts(String keyword, int limit, boolean recentPosts) {
        if (keyword == null || keyword.isBlank()) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return withBrowserRecovery(() -> extractor.searchPosts(keyword.trim(), safeLimit, recentPosts));
    }

    private <T> T withBrowserRecovery(Supplier<T> action) {
        try {
            return action.get();
        } catch (FacebookBrowserNavigationException stalledBrowser) {
            log.warn("Facebook navigation stalled; restarting the project browser once: {}",
                    stalledBrowser.getMessage());
            browserSession.retry();
            return action.get();
        }
    }

    private void persistNewComments(String postUrl, List<CommentDTO> comments) {
        List<Comment> fresh = new ArrayList<>();
        for (CommentDTO dto : comments) {
            if (!commentRepository.existsByPostUrlAndFbId(postUrl, dto.getId())) {
                fresh.add(Comment.builder().fbId(dto.getId()).postUrl(postUrl).author(dto.getAuthor()).authorUrl(dto.getAuthorUrl())
                        .message(dto.getMessage()).createdAt(dto.getCreatedAt()).parentFbId(dto.getParentId()).build());
            }
        }
        if (!fresh.isEmpty()) commentRepository.saveAll(fresh);
    }

    private ExtractResponseDTO response(String status, String message, List<CommentDTO> comments) {
        return ExtractResponseDTO.builder().status(status).message(message).comments(comments).build();
    }
}
