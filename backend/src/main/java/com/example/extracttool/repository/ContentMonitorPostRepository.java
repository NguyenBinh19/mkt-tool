package com.example.extracttool.repository;
import com.example.extracttool.model.entity.ContentMonitorPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentMonitorPostRepository extends JpaRepository<ContentMonitorPost, Long> {
    List<ContentMonitorPost> findByWorkspaceIdOrderBySearchedAtDesc(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
    Optional<ContentMonitorPost> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
