package com.example.extracttool.service;
import com.example.extracttool.model.dto.WorkspaceDataDtos.*;
import com.example.extracttool.model.entity.UserAccount;
import java.util.List;
public interface WorkspaceDataService {
    List<WorkspaceResponse> listWorkspaces(UserAccount user); WorkspaceResponse createWorkspace(UserAccount user, WorkspaceRequest request); WorkspaceResponse updateWorkspace(UserAccount user, Long id, WorkspaceRequest request); void deleteWorkspace(UserAccount user, Long id);
    List<LeadResponse> leads(UserAccount user, Long workspaceId); LeadResponse saveLead(UserAccount user, Long workspaceId, LeadRequest request); void deleteLead(UserAccount user, Long workspaceId, Long leadId); void clearLeads(UserAccount user, Long workspaceId);
    List<ExtractionHistoryResponse> history(UserAccount user, Long workspaceId); ExtractionHistoryResponse addHistory(UserAccount user, Long workspaceId, ExtractionHistoryRequest request); void clearHistory(UserAccount user, Long workspaceId);
    MonitorSnapshot monitor(UserAccount user, Long workspaceId); MonitorSnapshot replaceMonitorResults(UserAccount user, Long workspaceId, MonitorSearchRequest request); void clearMonitor(UserAccount user, Long workspaceId);
    MonitorPostResponse toggleMonitorStar(UserAccount user, Long workspaceId, Long postId); void clearMonitorResults(UserAccount user, Long workspaceId);
}
