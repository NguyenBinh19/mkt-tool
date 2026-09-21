package com.example.extracttool.repository;
import com.example.extracttool.model.entity.SavedLead;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SavedLeadRepository extends JpaRepository<SavedLead, Long> {
    List<SavedLead> findByWorkspaceIdOrderBySavedAtDesc(Long workspaceId);
    Optional<SavedLead> findByWorkspaceIdAndExternalCommentId(Long workspaceId, String externalCommentId);
    Optional<SavedLead> findByIdAndWorkspaceId(Long id, Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
