package com.example.extracttool.repository;

import com.example.extracttool.model.entity.ExtractionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, String> {
    Optional<ExtractionJob> findByIdAndOwnerId(String id, Long ownerId);
    Optional<ExtractionJob> findTopByWorkspaceIdAndOwnerIdOrderByCreatedAtDesc(Long workspaceId, Long ownerId);
    Slice<ExtractionJob> findByWorkspaceIdAndOwnerId(Long workspaceId, Long ownerId, Pageable pageable);
    long countByWorkspaceIdAndOwnerIdAndStatusIn(Long workspaceId, Long ownerId, List<ExtractionJob.Status> statuses);
    List<ExtractionJob> findByStatusInOrderByCreatedAtAsc(List<ExtractionJob.Status> statuses);
    List<ExtractionJob> findByWorkspaceIdAndOwnerIdAndPostUrlAndStatusOrderByCreatedAtDesc(Long workspaceId, Long ownerId, String postUrl, ExtractionJob.Status status);
    @Transactional
    void deleteByWorkspaceIdAndOwnerIdAndStatusIn(Long workspaceId, Long ownerId, List<ExtractionJob.Status> statuses);
}
