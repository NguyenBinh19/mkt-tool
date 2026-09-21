package com.example.extracttool.repository;
import com.example.extracttool.model.entity.FeatureAccessRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FeatureAccessRequestRepository extends JpaRepository<FeatureAccessRequest, Long> {
    List<FeatureAccessRequest> findByStatus(FeatureAccessRequest.RequestStatus status);
    boolean existsByUserIdAndFeatureKeyAndStatus(Long userId, String featureKey, FeatureAccessRequest.RequestStatus status);
}
