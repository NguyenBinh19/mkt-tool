package com.example.extracttool.service.impl;

import com.example.extracttool.config.FacebookProperties;
import com.example.extracttool.exception.AppException;
import com.example.extracttool.exception.ErrorCode;
import com.example.extracttool.model.dto.ExtractRequestDTO;
import com.example.extracttool.model.dto.ExtractResponseDTO;
import com.example.extracttool.model.dto.ExtractionJobDtos;
import com.example.extracttool.model.dto.WorkspaceDataDtos.ExtractionHistoryRequest;
import com.example.extracttool.model.entity.ExtractionJob;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.repository.ExtractionJobRepository;
import com.example.extracttool.repository.UserAccountRepository;
import com.example.extracttool.repository.WorkspaceRepository;
import com.example.extracttool.service.ExtractionJobService;
import com.example.extracttool.service.FacebookCommentService;
import com.example.extracttool.service.WorkspaceDataService;
import com.example.extracttool.validation.FacebookPostUrlValidator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ExtractionJobServiceImpl implements ExtractionJobService {
    private static final int MAX_QUEUED_JOBS = 100;
    private static final int JOB_PAGE_SIZE = 20;
    private final ExtractionJobRepository jobs;
    private final WorkspaceRepository workspaces;
    private final UserAccountRepository users;
    private final FacebookCommentService facebook;
    private final WorkspaceDataService workspaceData;
    private final FacebookPostUrlValidator urlValidator;
    private final FacebookProperties properties;
    private final ObjectMapper mapper;
    private final ThreadPoolExecutor workers;

    public ExtractionJobServiceImpl(ExtractionJobRepository jobs, WorkspaceRepository workspaces,
                                    UserAccountRepository users, FacebookCommentService facebook,
                                    WorkspaceDataService workspaceData, FacebookPostUrlValidator urlValidator,
                                    FacebookProperties properties, ObjectMapper mapper) {
        this.jobs = jobs;
        this.workspaces = workspaces;
        this.users = users;
        this.facebook = facebook;
        this.workspaceData = workspaceData;
        this.urlValidator = urlValidator;
        this.properties = properties;
        this.mapper = mapper;
        int concurrency = properties.getMaxConcurrentTabs();
        if (concurrency < 1) throw new IllegalArgumentException("facebook.max-concurrent-tabs must be at least 1");
        this.workers = new ThreadPoolExecutor(concurrency, concurrency, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUED_JOBS), runnable -> {
                Thread thread = new Thread(runnable, "facebook-extraction-worker");
                thread.setDaemon(true);
                return thread;
            });
    }

    @PostConstruct
    void resumeInterruptedJobs() {
        for (ExtractionJob job : jobs.findByStatusInOrderByCreatedAtAsc(List.of(ExtractionJob.Status.QUEUED, ExtractionJob.Status.RUNNING))) {
            job.setStatus(ExtractionJob.Status.QUEUED);
            jobs.save(job);
            schedule(job.getId());
        }
    }

    @PreDestroy
    public void stopWorkers() { workers.shutdownNow(); }

    @Override
    public ExtractionJobDtos.Snapshot submit(UserAccount user, ExtractionJobDtos.Submit request) {
        if (workspaces.findByIdAndOwnerId(request.workspaceId(), user.getId()).isEmpty()) throw new AppException(ErrorCode.WORKSPACE_NOT_FOUND);
        if (urlValidator.validate(request.postUrl()) != null) throw new AppException(ErrorCode.VALIDATION_ERROR);
        int max = request.maxComments() == null ? 200 : request.maxComments();
        if (max < 1 || max > properties.getMaxCommentsLimit()) throw new AppException(ErrorCode.VALIDATION_ERROR);
        ExtractionJob job = jobs.save(new ExtractionJob(user.getId(), request.workspaceId(), request.postUrl().trim(), max, request.includeReplies()));
        try {
            schedule(job.getId());
        } catch (RuntimeException exception) {
            jobs.deleteById(job.getId());
            throw new AppException(ErrorCode.EXTRACTION_QUEUE_FULL);
        }
        return snapshot(job);
    }

    @Override
    public ExtractionJobDtos.Snapshot get(UserAccount user, String id) {
        return snapshot(jobs.findByIdAndOwnerId(id, user.getId()).orElseThrow(() -> new AppException(ErrorCode.EXTRACTION_JOB_NOT_FOUND)));
    }

    @Override
    public ExtractionJobDtos.Snapshot latest(UserAccount user, Long workspaceId) {
        if (workspaces.findByIdAndOwnerId(workspaceId, user.getId()).isEmpty()) throw new AppException(ErrorCode.WORKSPACE_NOT_FOUND);
        return jobs.findTopByWorkspaceIdAndOwnerIdOrderByCreatedAtDesc(workspaceId, user.getId()).map(this::snapshot).orElse(null);
    }

    @Override
    public ExtractionJobDtos.JobPage list(UserAccount user, Long workspaceId, int page) {
        if (workspaces.findByIdAndOwnerId(workspaceId, user.getId()).isEmpty()) throw new AppException(ErrorCode.WORKSPACE_NOT_FOUND);
        if (page < 0) throw new AppException(ErrorCode.VALIDATION_ERROR);
        var slice = jobs.findByWorkspaceIdAndOwnerId(workspaceId, user.getId(),
                PageRequest.of(page, JOB_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        var items = slice.getContent().stream()
                .map(job -> new ExtractionJobDtos.Summary(job.getId(), job.getStatus(), job.getPostUrl(),
                        job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(), job.getErrorMessage())).toList();
        long activeCount = slice.hasNext()
                ? jobs.countByWorkspaceIdAndOwnerIdAndStatusIn(workspaceId, user.getId(),
                        List.of(ExtractionJob.Status.QUEUED, ExtractionJob.Status.RUNNING))
                : items.stream().filter(job -> job.status() == ExtractionJob.Status.QUEUED || job.status() == ExtractionJob.Status.RUNNING).count();
        return new ExtractionJobDtos.JobPage(items, slice.hasNext(), activeCount);
    }

    @Override
    public void clearFinished(UserAccount user, Long workspaceId) {
        if (workspaces.findByIdAndOwnerId(workspaceId, user.getId()).isEmpty()) throw new AppException(ErrorCode.WORKSPACE_NOT_FOUND);
        jobs.deleteByWorkspaceIdAndOwnerIdAndStatusIn(workspaceId, user.getId(),
                List.of(ExtractionJob.Status.COMPLETED, ExtractionJob.Status.FAILED));
    }

    private void schedule(String id) { workers.execute(() -> process(id)); }

    private void process(String id) {
        ExtractionJob job = jobs.findById(id).orElse(null);
        if (job == null) return;
        job.setStatus(ExtractionJob.Status.RUNNING);
        job.setStartedAt(Instant.now());
        jobs.save(job);
        try {
            ExtractResponseDTO result = facebook.extractComments(new ExtractRequestDTO(job.getPostUrl(), job.getMaxComments(), job.isIncludeReplies()));
            job.setResultJson(mapper.writeValueAsString(result));
            if ("ok".equals(result.getStatus())) {
                UserAccount owner = users.findById(job.getOwnerId()).orElseThrow();
                workspaceData.addHistory(owner, job.getWorkspaceId(), new ExtractionHistoryRequest(job.getPostUrl(), result.getComments().size(), result.getComments()));
                job.setStatus(ExtractionJob.Status.COMPLETED);
            } else {
                job.setStatus(ExtractionJob.Status.FAILED);
                job.setErrorMessage(trimError(result.getMessage()));
            }
        } catch (Exception exception) {
            job.setStatus(ExtractionJob.Status.FAILED);
            job.setErrorMessage(trimError(exception.getMessage()));
        } finally {
            job.setFinishedAt(Instant.now());
            jobs.save(job);
        }
    }

    private ExtractionJobDtos.Snapshot snapshot(ExtractionJob job) {
        ExtractResponseDTO result = job.getResultJson() == null ? null : mapper.readValue(job.getResultJson(), ExtractResponseDTO.class);
        return new ExtractionJobDtos.Snapshot(job.getId(), job.getStatus(), job.getPostUrl(), job.getCreatedAt(),
                job.getStartedAt(), job.getFinishedAt(), result, job.getErrorMessage());
    }

    private String trimError(String message) {
        if (message == null) return "Extraction failed";
        return message.substring(0, Math.min(2000, message.length()));
    }
}
