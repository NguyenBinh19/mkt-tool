package com.example.extracttool.repository;
import com.example.extracttool.model.entity.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    Optional<Workspace> findByIdAndOwnerId(Long id, Long ownerId);
}
