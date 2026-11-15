package com.example.extracttool.repository;
import com.example.extracttool.model.entity.ExtractionHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExtractionHistoryRepository extends JpaRepository<ExtractionHistory, Long> {
    List<ExtractionHistory> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<ExtractionHistory> findByIdAndWorkspaceId(Long id, Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
