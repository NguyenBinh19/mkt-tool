package com.example.extracttool.controller;

import com.example.extracttool.model.dto.WorkspaceDataDtos.*;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.service.AccessControlService;
import com.example.extracttool.service.WorkspaceDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceDataController {
    private final WorkspaceDataService data;
    private final AccessControlService access;

    @GetMapping public List<WorkspaceResponse> list(HttpServletRequest request) { return data.listWorkspaces(current(request)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public WorkspaceResponse create(@Valid @RequestBody WorkspaceRequest body, HttpServletRequest request) { return data.createWorkspace(current(request), body); }
    @PutMapping("/{workspaceId}") public WorkspaceResponse update(@PathVariable Long workspaceId, @Valid @RequestBody WorkspaceRequest body, HttpServletRequest request) { return data.updateWorkspace(current(request), workspaceId, body); }
    @DeleteMapping("/{workspaceId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long workspaceId, HttpServletRequest request) { data.deleteWorkspace(current(request), workspaceId); }
    @GetMapping("/{workspaceId}/leads") public List<LeadResponse> leads(@PathVariable Long workspaceId, HttpServletRequest request) { return data.leads(current(request), workspaceId); }
    @PostMapping("/{workspaceId}/leads") public LeadResponse saveLead(@PathVariable Long workspaceId, @Valid @RequestBody LeadRequest body, HttpServletRequest request) { return data.saveLead(current(request), workspaceId, body); }
    @DeleteMapping("/{workspaceId}/leads/{leadId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteLead(@PathVariable Long workspaceId, @PathVariable Long leadId, HttpServletRequest request) { data.deleteLead(current(request), workspaceId, leadId); }
    @DeleteMapping("/{workspaceId}/leads") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearLeads(@PathVariable Long workspaceId, HttpServletRequest request) { data.clearLeads(current(request), workspaceId); }
    @GetMapping("/{workspaceId}/history") public List<ExtractionHistoryResponse> history(@PathVariable Long workspaceId, HttpServletRequest request) { return data.history(current(request), workspaceId); }
    @GetMapping("/{workspaceId}/history/{historyId}") public ExtractionHistoryDetailResponse historyDetail(@PathVariable Long workspaceId, @PathVariable Long historyId, HttpServletRequest request) { return data.historyDetail(current(request), workspaceId, historyId); }
    @GetMapping("/{workspaceId}/suggested-comments") public List<SuggestedCommentResponse> suggestedComments(@PathVariable Long workspaceId, HttpServletRequest request) { return data.suggestedComments(current(request), workspaceId); }
    @PostMapping("/{workspaceId}/history") public ExtractionHistoryResponse history(@PathVariable Long workspaceId, @Valid @RequestBody ExtractionHistoryRequest body, HttpServletRequest request) { return data.addHistory(current(request), workspaceId, body); }
    @DeleteMapping("/{workspaceId}/history") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearHistory(@PathVariable Long workspaceId, HttpServletRequest request) { data.clearHistory(current(request), workspaceId); }
    @GetMapping("/{workspaceId}/monitor") public MonitorSnapshot monitor(@PathVariable Long workspaceId, HttpServletRequest request) { return data.monitor(current(request), workspaceId); }
    @PostMapping("/{workspaceId}/monitor/search") public MonitorSnapshot saveMonitor(@PathVariable Long workspaceId, @Valid @RequestBody MonitorSearchRequest body, HttpServletRequest request) { return data.replaceMonitorResults(current(request), workspaceId, body); }
    @DeleteMapping("/{workspaceId}/monitor") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearMonitor(@PathVariable Long workspaceId, HttpServletRequest request) { data.clearMonitor(current(request), workspaceId); }
    @PatchMapping("/{workspaceId}/monitor/posts/{postId}/star") public MonitorPostResponse toggleMonitorStar(@PathVariable Long workspaceId, @PathVariable Long postId, HttpServletRequest request) { return data.toggleMonitorStar(current(request), workspaceId, postId); }
    @DeleteMapping("/{workspaceId}/monitor/results") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearMonitorResults(@PathVariable Long workspaceId, HttpServletRequest request) { data.clearMonitorResults(current(request), workspaceId); }

    private UserAccount current(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return access.require(header == null ? "" : header.replace("Bearer ", ""));
    }
}
