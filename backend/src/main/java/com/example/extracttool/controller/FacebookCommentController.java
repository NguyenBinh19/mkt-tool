package com.example.extracttool.controller;

import com.example.extracttool.model.dto.ExtractRequestDTO;
import com.example.extracttool.model.dto.ExtractResponseDTO;
import com.example.extracttool.model.dto.FacebookSearchRequest;
import com.example.extracttool.service.FacebookCommentService;
import com.example.extracttool.service.ExtractionJobService;
import com.example.extracttool.model.dto.ExtractionJobDtos;
import com.example.extracttool.model.dto.FacebookBrowserStatusDTO;
import com.example.extracttool.config.BrowserSession;
import com.example.extracttool.exception.AppException;
import com.example.extracttool.exception.ErrorCode;
import com.example.extracttool.service.AccessControlService;
import com.example.extracttool.model.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facebook")
@RequiredArgsConstructor
@Slf4j
public class FacebookCommentController {
    private final FacebookCommentService service;
    private final AccessControlService access;
    private final ExtractionJobService jobs;
    private final BrowserSession browserSession;

    @PostMapping("/extract-jobs")
    public ResponseEntity<ExtractionJobDtos.Snapshot> submitJob(@Valid @RequestBody ExtractionJobDtos.Submit request, HttpServletRequest httpRequest) {
        UserAccount user = current(httpRequest);
        access.requireFeature(user, "Extract comment");
        return ResponseEntity.accepted().body(jobs.submit(user, request));
    }

    @GetMapping("/extract-jobs/{id}")
    public ResponseEntity<ExtractionJobDtos.Snapshot> job(@PathVariable String id, HttpServletRequest httpRequest) {
        UserAccount user = current(httpRequest);
        access.requireFeature(user, "Extract comment");
        return ResponseEntity.ok(jobs.get(user, id));
    }

    @GetMapping("/extract-jobs/latest")
    public ResponseEntity<ExtractionJobDtos.Snapshot> latestJob(@RequestParam Long workspaceId, HttpServletRequest httpRequest) {
        UserAccount user = current(httpRequest);
        access.requireFeature(user, "Extract comment");
        ExtractionJobDtos.Snapshot latest = jobs.latest(user, workspaceId);
        return latest == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(latest);
    }

    @GetMapping("/extract-jobs")
    public ExtractionJobDtos.JobPage listJobs(@RequestParam Long workspaceId, @RequestParam(defaultValue = "0") int page, HttpServletRequest httpRequest) {
        UserAccount user = current(httpRequest);
        access.requireFeature(user, "Extract comment");
        return jobs.list(user, workspaceId, page);
    }

    @DeleteMapping("/extract-jobs")
    public ResponseEntity<Void> clearJobs(@RequestParam Long workspaceId, HttpServletRequest httpRequest) {
        UserAccount user = current(httpRequest);
        access.requireFeature(user, "Extract comment");
        jobs.clearFinished(user, workspaceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/extract")
    public ResponseEntity<ExtractResponseDTO> extractComments(@Valid @RequestBody ExtractRequestDTO request, HttpServletRequest httpRequest) {
        access.requireFeature(current(httpRequest), "Extract comment");
        ExtractResponseDTO resp = service.extractComments(request);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/browser/open")
    public ResponseEntity<String> openBrowserForLogin(HttpServletRequest httpRequest) {
        access.requireAdmin(current(httpRequest));
        try {
            service.openBrowserForLogin();
        } catch (RuntimeException exception) {
            log.warn("Could not open Facebook login tab", exception);
            throw new AppException(ErrorCode.BROWSER_UNAVAILABLE);
        }
        return ResponseEntity.ok("The Facebook browser is open on the server. An administrator can sign in through the private remote display.");
    }

    @GetMapping("/browser/status")
    public FacebookBrowserStatusDTO browserStatus(HttpServletRequest httpRequest) {
        access.requireAdmin(current(httpRequest));
        return browserSession.status();
    }

    @PostMapping("/browser/retry")
    public FacebookBrowserStatusDTO retryBrowser(HttpServletRequest httpRequest) {
        access.requireAdmin(current(httpRequest));
        try {
            browserSession.retry();
        } catch (AppException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Could not restart Facebook browser", exception);
            throw new AppException(ErrorCode.BROWSER_UNAVAILABLE);
        }
        return browserSession.status();
    }

    @PostMapping("/search")
    public ResponseEntity<java.util.List<com.example.extracttool.model.dto.FacebookPostDTO>> searchPosts(@Valid @RequestBody FacebookSearchRequest request, HttpServletRequest httpRequest) {
        access.requireFeature(current(httpRequest), "Content monitor");
        return ResponseEntity.ok(service.searchPosts(request.getKeyword(), request.getLimit() == null ? 20 : request.getLimit(), request.isRecentPosts()));
    }

    private UserAccount current(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return access.require(header == null ? "" : header.replace("Bearer ", ""));
    }
}
