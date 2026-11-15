package com.example.extracttool.service;

import com.example.extracttool.model.dto.WorkspaceDataDtos.WorkspaceRequest;
import com.example.extracttool.model.dto.WorkspaceDataDtos.ExtractionHistoryRequest;
import com.example.extracttool.model.dto.CommentDTO;
import com.example.extracttool.model.entity.ExtractionHistory;
import com.example.extracttool.exception.AppException;
import com.example.extracttool.exception.ErrorCode;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.model.entity.Workspace;
import com.example.extracttool.repository.*;
import com.example.extracttool.service.impl.WorkspaceDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceDataServiceTest {
    @Mock WorkspaceRepository workspaces;
    @Mock SavedLeadRepository leads;
    @Mock ExtractionHistoryRepository extractionHistory;
    @Mock ContentMonitorPostRepository monitorPosts;
    @Mock ContentMonitorHistoryRepository monitorHistory;
    @Mock ExtractionJobRepository extractionJobs;
    @Spy ObjectMapper mapper = new ObjectMapper();
    @InjectMocks WorkspaceDataServiceImpl service;

    @Test
    void listCreatesDefaultWorkspaceForNewUser() {
        UserAccount user = user(12L);
        Workspace saved = new Workspace(user, "Marketing workspace", "Không gian tăng trưởng", "Theo dõi tín hiệu thị trường và cơ hội tiếp cận.");
        saved.setId(40L);
        when(workspaces.findByOwnerIdOrderByUpdatedAtDesc(12L)).thenReturn(List.of());
        when(workspaces.save(any(Workspace.class))).thenReturn(saved);

        var result = service.listWorkspaces(user);

        assertThat(result).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(40L));
        verify(workspaces).save(any(Workspace.class));
    }

    @Test
    void createTrimsWorkspaceFields() {
        UserAccount user = user(12L);
        Workspace saved = new Workspace(user, "Mẹ và bé", "Bán lẻ", "Nhu cầu chăm sóc trẻ");
        saved.setId(41L);
        when(workspaces.save(any(Workspace.class))).thenReturn(saved);

        service.createWorkspace(user, new WorkspaceRequest("  Mẹ và bé ", " Bán lẻ ", " Nhu cầu chăm sóc trẻ "));

        ArgumentCaptor<Workspace> captor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaces).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Mẹ và bé");
        assertThat(captor.getValue().getIndustry()).isEqualTo("Bán lẻ");
    }

    @Test
    void historyStoresCommentsAndReturnsThemForTheSameWorkspace() {
        UserAccount user = user(12L);
        Workspace workspace = workspace(user, 40L);
        CommentDTO comment = new CommentDTO("fb-1", "An", "https://facebook.com/an", "Cho mình xin giá", Instant.now(), null);
        when(workspaces.findByIdAndOwnerId(40L, 12L)).thenReturn(Optional.of(workspace));
        when(extractionHistory.save(any(ExtractionHistory.class))).thenAnswer(call -> {
            ExtractionHistory entry = call.getArgument(0);
            entry.setId(7L);
            entry.setCreatedAt(Instant.now());
            return entry;
        });

        service.addHistory(user, 40L, new ExtractionHistoryRequest(" https://facebook.com/post ", 1, List.of(comment)));

        ArgumentCaptor<ExtractionHistory> captor = ArgumentCaptor.forClass(ExtractionHistory.class);
        verify(extractionHistory).save(captor.capture());
        ExtractionHistory stored = captor.getValue();
        assertThat(stored.getPostUrl()).isEqualTo("https://facebook.com/post");
        assertThat(stored.getResultJson()).contains("Cho mình xin giá");
        when(extractionHistory.findByIdAndWorkspaceId(7L, 40L)).thenReturn(Optional.of(stored));
        var detail = service.historyDetail(user, 40L, 7L);
        assertThat(detail.comments()).extracting(CommentDTO::getMessage).containsExactly("Cho mình xin giá");
        assertThat(detail.legacyResult()).isFalse();
        verifyNoInteractions(extractionJobs);
    }

    @Test
    void automaticSuggestionsUseOnlyMatchingCommentsFromTheWorkspace() {
        UserAccount user = user(12L);
        Workspace workspace = workspace(user, 40L);
        ExtractionHistory entry = new ExtractionHistory(workspace, "https://facebook.com/post", 2);
        entry.setId(7L);
        entry.setCreatedAt(Instant.now());
        entry.setResultJson(mapper.writeValueAsString(List.of(
                new CommentDTO("fb-1", "An", null, "Cho mình xin giá", null, null),
                new CommentDTO("fb-2", "Bình", null, "Bài viết hay", null, null))));
        when(workspaces.findByIdAndOwnerId(40L, 12L)).thenReturn(Optional.of(workspace));
        when(extractionHistory.findByWorkspaceIdOrderByCreatedAtDesc(40L)).thenReturn(List.of(entry));

        var suggestions = service.suggestedComments(user, 40L);

        assertThat(suggestions).singleElement().satisfies(item -> {
            assertThat(item.comment().getId()).isEqualTo("fb-1");
            assertThat(item.postUrl()).isEqualTo("https://facebook.com/post");
        });
    }

    @Test
    void historyDetailCannotReadAnEntryOutsideTheSelectedWorkspace() {
        UserAccount user = user(12L);
        when(workspaces.findByIdAndOwnerId(40L, 12L)).thenReturn(Optional.of(workspace(user, 40L)));
        when(extractionHistory.findByIdAndWorkspaceId(7L, 40L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.historyDetail(user, 40L, 7L))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTRACTION_HISTORY_NOT_FOUND));
    }

    private Workspace workspace(UserAccount user, Long id) { Workspace workspace = new Workspace(user, "Demo", "Retail", ""); workspace.setId(id); return workspace; }
    private UserAccount user(Long id) { UserAccount user = new UserAccount("Demo", "demo@example.com", "hash"); user.setId(id); return user; }
}
