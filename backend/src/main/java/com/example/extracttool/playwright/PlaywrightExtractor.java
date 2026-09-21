package com.example.extracttool.playwright;

import com.example.extracttool.model.dto.CommentDTO;
import java.util.List;

public interface PlaywrightExtractor {
    List<CommentDTO> extract(String postUrl, int maxComments, boolean includeReplies);
    List<com.example.extracttool.model.dto.FacebookPostDTO> searchPosts(String keyword, int limit, boolean recentPosts);
}
