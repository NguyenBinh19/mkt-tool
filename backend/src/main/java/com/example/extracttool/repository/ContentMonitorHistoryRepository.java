package com.example.extracttool.repository;
import com.example.extracttool.model.entity.ContentMonitorHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentMonitorHistoryRepository extends JpaRepository<ContentMonitorHistory, Long> {
    List<ContentMonitorHistory> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
