package com.example.extracttool.service;

public interface FacebookCommentService {
    com.example.extracttool.model.dto.ExtractResponseDTO extractComments(com.example.extracttool.model.dto.ExtractRequestDTO request);
    void openBrowserForLogin();
}
