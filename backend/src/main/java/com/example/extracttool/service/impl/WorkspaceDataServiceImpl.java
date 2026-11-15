package com.example.extracttool.service.impl;

import com.example.extracttool.exception.AppException;
import com.example.extracttool.exception.ErrorCode;
import com.example.extracttool.model.dto.CommentDTO;
import com.example.extracttool.model.dto.ExtractResponseDTO;
import com.example.extracttool.model.dto.WorkspaceDataDtos.*;
import com.example.extracttool.model.entity.*;
import com.example.extracttool.repository.*;
import com.example.extracttool.service.WorkspaceDataService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceDataServiceImpl implements WorkspaceDataService {
    private static final Pattern INTEREST_SIGNAL = Pattern.compile("giá|bao nhiêu|inbox|tư vấn|mua|đặt|quan tâm|liên hệ|sđt|phone|\\bib\\b|ship", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private final WorkspaceRepository workspaces;
    private final SavedLeadRepository leads;
    private final ExtractionHistoryRepository extractionHistory;
    private final ContentMonitorPostRepository monitorPosts;
    private final ContentMonitorHistoryRepository monitorHistory;
    private final ExtractionJobRepository extractionJobs;
    private final ObjectMapper mapper;

    @Override public List<WorkspaceResponse> listWorkspaces(UserAccount user) {
        var values = workspaces.findByOwnerIdOrderByUpdatedAtDesc(user.getId());
        if (values.isEmpty()) values = List.of(workspaces.save(new Workspace(user, "Marketing workspace", "Không gian tăng trưởng", "Theo dõi tín hiệu thị trường và cơ hội tiếp cận.")));
        return values.stream().map(this::workspace).toList();
    }
    @Override public WorkspaceResponse createWorkspace(UserAccount user, WorkspaceRequest request) {
        return workspace(workspaces.save(new Workspace(user, request.name().trim(), request.industry().trim(), clean(request.description()))));
    }
    @Override public WorkspaceResponse updateWorkspace(UserAccount user, Long id, WorkspaceRequest request) {
        Workspace workspace = owned(user, id);
        workspace.setName(request.name().trim()); workspace.setIndustry(request.industry().trim()); workspace.setDescription(clean(request.description()));
        return workspace(workspace);
    }
    @Override public void deleteWorkspace(UserAccount user, Long id) {
        Workspace workspace = owned(user, id);
        if (workspaces.findByOwnerIdOrderByUpdatedAtDesc(user.getId()).size() <= 1) throw new AppException(ErrorCode.WORKSPACE_REQUIRED);
        leads.deleteByWorkspaceId(id); extractionHistory.deleteByWorkspaceId(id); monitorPosts.deleteByWorkspaceId(id); monitorHistory.deleteByWorkspaceId(id); workspaces.delete(workspace);
    }
    @Override public List<LeadResponse> leads(UserAccount user, Long workspaceId) { owned(user, workspaceId); return leads.findByWorkspaceIdOrderBySavedAtDesc(workspaceId).stream().map(this::lead).toList(); }
    @Override public LeadResponse saveLead(UserAccount user, Long workspaceId, LeadRequest request) {
        SavedLead lead = leads.findByWorkspaceIdAndExternalCommentId(workspaceId, request.externalCommentId()).orElseGet(() -> new SavedLead(owned(user, workspaceId), request.externalCommentId()));
        lead.setAuthor(clean(request.author())); lead.setAuthorUrl(clean(request.authorUrl())); lead.setPostUrl(clean(request.postUrl())); lead.setMessage(clean(request.message())); lead.setParentCommentId(clean(request.parentCommentId())); lead.setCommentCreatedAt(request.commentCreatedAt());
        return lead(leads.save(lead));
    }
    @Override public void deleteLead(UserAccount user, Long workspaceId, Long leadId) { owned(user, workspaceId); leads.delete(leads.findByIdAndWorkspaceId(leadId, workspaceId).orElseThrow(() -> new AppException(ErrorCode.LEAD_NOT_FOUND))); }
    @Override public void clearLeads(UserAccount user, Long workspaceId) { owned(user, workspaceId); leads.deleteByWorkspaceId(workspaceId); }
    @Override public List<ExtractionHistoryResponse> history(UserAccount user, Long workspaceId) { owned(user, workspaceId); return extractionHistory.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream().map(this::history).toList(); }
    @Override public ExtractionHistoryResponse addHistory(UserAccount user, Long workspaceId, ExtractionHistoryRequest request) {
        ExtractionHistory entry = new ExtractionHistory(owned(user, workspaceId), request.postUrl().trim(), request.commentCount());
        if (request.comments() != null) entry.setResultJson(mapper.writeValueAsString(request.comments()));
        return history(extractionHistory.save(entry));
    }
    @Override public ExtractionHistoryDetailResponse historyDetail(UserAccount user, Long workspaceId, Long historyId) {
        owned(user, workspaceId);
        ExtractionHistory entry = extractionHistory.findByIdAndWorkspaceId(historyId, workspaceId)
                .orElseThrow(() -> new AppException(ErrorCode.EXTRACTION_HISTORY_NOT_FOUND));
        boolean legacy = entry.getResultJson() == null;
        return new ExtractionHistoryDetailResponse(entry.getId(), entry.getPostUrl(), entry.getCommentCount(),
                entry.getCreatedAt(), legacy ? legacyComments(user, entry) : parseComments(entry.getResultJson()), legacy);
    }
    @Override public List<SuggestedCommentResponse> suggestedComments(UserAccount user, Long workspaceId) {
        owned(user, workspaceId);
        Map<String, SuggestedCommentResponse> suggestions = new LinkedHashMap<>();
        for (ExtractionHistory entry : extractionHistory.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)) {
            List<CommentDTO> comments = entry.getResultJson() == null ? legacyComments(user, entry) : parseComments(entry.getResultJson());
            for (CommentDTO comment : comments) {
                if (comment.getId() == null || !INTEREST_SIGNAL.matcher(comment.getMessage() == null ? "" : comment.getMessage()).find()) continue;
                suggestions.putIfAbsent(comment.getId(), new SuggestedCommentResponse(entry.getId(), entry.getPostUrl(), entry.getCreatedAt(), comment));
            }
        }
        return List.copyOf(suggestions.values());
    }
    @Override public void clearHistory(UserAccount user, Long workspaceId) { owned(user, workspaceId); extractionHistory.deleteByWorkspaceId(workspaceId); }
    @Override public MonitorSnapshot monitor(UserAccount user, Long workspaceId) { owned(user, workspaceId); return new MonitorSnapshot(monitorPosts.findByWorkspaceIdOrderBySearchedAtDesc(workspaceId).stream().map(this::post).toList(), monitorHistory.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream().map(this::monitor).toList()); }
    @Override public MonitorSnapshot replaceMonitorResults(UserAccount user, Long workspaceId, MonitorSearchRequest request) {
        Workspace workspace = owned(user, workspaceId); monitorPosts.deleteByWorkspaceId(workspaceId); monitorPosts.flush();
        var posts = request.results().stream().map(item -> { String postUrl = item.url().trim(); ContentMonitorPost post = new ContentMonitorPost(workspace, postUrl); post.setPostUrlHash(sha256(postUrl)); post.setAuthor(clean(item.author())); post.setContent(clean(item.text())); return post; }).toList();
        monitorPosts.saveAll(posts); monitorHistory.save(new ContentMonitorHistory(workspace, request.keyword().trim(), posts.size())); return monitor(user, workspaceId);
    }
    @Override public void clearMonitor(UserAccount user, Long workspaceId) { owned(user, workspaceId); monitorPosts.deleteByWorkspaceId(workspaceId); monitorHistory.deleteByWorkspaceId(workspaceId); }
    @Override public MonitorPostResponse toggleMonitorStar(UserAccount user, Long workspaceId, Long postId) { owned(user, workspaceId); ContentMonitorPost post = monitorPosts.findByIdAndWorkspaceId(postId, workspaceId).orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND)); post.setStarred(!post.isStarred()); return post(post); }
    @Override public void clearMonitorResults(UserAccount user, Long workspaceId) { owned(user, workspaceId); monitorPosts.deleteByWorkspaceId(workspaceId); }
    private Workspace owned(UserAccount user, Long id) { return workspaces.findByIdAndOwnerId(id, user.getId()).orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND)); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(64);
            for (byte valueByte : bytes) hash.append(String.format("%02x", valueByte));
            return hash.toString();
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
    private WorkspaceResponse workspace(Workspace value) { return new WorkspaceResponse(value.getId(), value.getName(), value.getIndustry(), value.getDescription(), value.getCreatedAt(), value.getUpdatedAt()); }
    private LeadResponse lead(SavedLead value) { return new LeadResponse(value.getId(), value.getExternalCommentId(), value.getAuthor(), value.getAuthorUrl(), value.getPostUrl(), value.getMessage(), value.getParentCommentId(), value.getCommentCreatedAt(), value.getSavedAt()); }
    private ExtractionHistoryResponse history(ExtractionHistory value) { return new ExtractionHistoryResponse(value.getId(), value.getPostUrl(), value.getCommentCount(), value.getCreatedAt()); }
    private List<CommentDTO> parseComments(String json) { return Arrays.asList(mapper.readValue(json, CommentDTO[].class)); }
    private List<CommentDTO> legacyComments(UserAccount user, ExtractionHistory entry) {
        return extractionJobs.findByWorkspaceIdAndOwnerIdAndPostUrlAndStatusOrderByCreatedAtDesc(
                        entry.getWorkspace().getId(), user.getId(), entry.getPostUrl(), ExtractionJob.Status.COMPLETED)
                .stream().filter(job -> job.getResultJson() != null)
                .min((left, right) -> Long.compare(distance(entry, left), distance(entry, right)))
                .map(job -> mapper.readValue(job.getResultJson(), ExtractResponseDTO.class))
                .map(result -> result.getComments() == null ? List.<CommentDTO>of() : result.getComments())
                .orElseGet(List::of);
    }
    private long distance(ExtractionHistory entry, ExtractionJob job) {
        return Math.abs(Duration.between(entry.getCreatedAt(), job.getFinishedAt() == null ? job.getCreatedAt() : job.getFinishedAt()).toMillis());
    }
    private MonitorPostResponse post(ContentMonitorPost value) { return new MonitorPostResponse(value.getId(), value.getPostUrl(), value.getAuthor(), value.getContent(), value.isStarred(), value.getSearchedAt()); }
    private MonitorHistoryResponse monitor(ContentMonitorHistory value) { return new MonitorHistoryResponse(value.getId(), value.getKeyword(), value.getResultCount(), value.getCreatedAt()); }
}
