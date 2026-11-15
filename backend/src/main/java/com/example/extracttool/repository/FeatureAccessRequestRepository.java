package com.example.extracttool.repository;
import com.example.extracttool.model.entity.FeatureAccessRequest;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FeatureAccessRequestRepository extends JpaRepository<FeatureAccessRequest, Long> {
    @EntityGraph(attributePaths = "user")
    List<FeatureAccessRequest> findByStatus(FeatureAccessRequest.RequestStatus status);
    @EntityGraph(attributePaths = "user")
    List<FeatureAccessRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndFeatureKeyAndStatus(Long userId, String featureKey, FeatureAccessRequest.RequestStatus status);
}
