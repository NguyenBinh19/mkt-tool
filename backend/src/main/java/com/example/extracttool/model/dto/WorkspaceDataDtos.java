package com.example.extracttool.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class WorkspaceDataDtos {
    private WorkspaceDataDtos() { }

    public record WorkspaceRequest(@NotBlank String name, @NotBlank String industry, String description) { }
    public record WorkspaceResponse(Long id, String name, String industry, String description, Instant createdAt, Instant updatedAt) { }
    public record LeadRequest(@NotBlank String externalCommentId, String author, String authorUrl, String message, String parentCommentId, Instant commentCreatedAt) { }
    public record LeadResponse(Long id, String externalCommentId, String author, String authorUrl, String message, String parentCommentId, Instant commentCreatedAt, Instant savedAt) { }
    public record ExtractionHistoryRequest(@NotBlank String postUrl, @NotNull Integer commentCount) { }
    public record ExtractionHistoryResponse(Long id, String postUrl, int commentCount, Instant createdAt) { }
    public record MonitorPostRequest(@NotBlank String url, String author, String text) { }
    public record MonitorSearchRequest(@NotBlank String keyword, @NotNull @Valid List<MonitorPostRequest> results) { }
    public record MonitorPostResponse(Long id, String url, String author, String text, boolean starred, Instant searchedAt) { }
    public record MonitorHistoryResponse(Long id, String keyword, int count, Instant createdAt) { }
    public record MonitorSnapshot(List<MonitorPostResponse> results, List<MonitorHistoryResponse> history) { }
}
