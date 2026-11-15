package com.example.extracttool.model.dto;

import com.example.extracttool.model.entity.ExtractionJob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class ExtractionJobDtos {
    private ExtractionJobDtos() { }
    public record Submit(@NotNull Long workspaceId, @NotBlank String postUrl, Integer maxComments, boolean includeReplies) { }
    public record Snapshot(String id, ExtractionJob.Status status, String postUrl, Instant createdAt,
                           Instant startedAt, Instant finishedAt, ExtractResponseDTO result, String error) { }
    public record Summary(String id, ExtractionJob.Status status, String postUrl, Instant createdAt,
                          Instant startedAt, Instant finishedAt, String error) { }
    public record JobPage(List<Summary> items, boolean hasMore, long activeCount) { }
}
