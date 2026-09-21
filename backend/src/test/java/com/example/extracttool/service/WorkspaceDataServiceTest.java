package com.example.extracttool.service;

import com.example.extracttool.model.dto.WorkspaceDataDtos.WorkspaceRequest;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.model.entity.Workspace;
import com.example.extracttool.repository.*;
import com.example.extracttool.service.impl.WorkspaceDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceDataServiceTest {
    @Mock WorkspaceRepository workspaces;
    @Mock SavedLeadRepository leads;
    @Mock ExtractionHistoryRepository extractionHistory;
    @Mock ContentMonitorPostRepository monitorPosts;
    @Mock ContentMonitorHistoryRepository monitorHistory;
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

    private UserAccount user(Long id) { UserAccount user = new UserAccount("Demo", "demo@example.com", "hash"); user.setId(id); return user; }
}
