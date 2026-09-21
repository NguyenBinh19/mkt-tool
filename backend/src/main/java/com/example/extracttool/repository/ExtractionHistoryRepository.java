package com.example.extracttool.repository;
import com.example.extracttool.model.entity.ExtractionHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExtractionHistoryRepository extends JpaRepository<ExtractionHistory, Long> {
    List<ExtractionHistory> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
