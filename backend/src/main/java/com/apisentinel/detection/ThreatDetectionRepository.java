package com.apisentinel.detection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ThreatDetection entity.
 */
@Repository
public interface ThreatDetectionRepository extends JpaRepository<ThreatDetection, Long> {

    List<ThreatDetection> findBySecurityEventId(Long securityEventId);

    List<ThreatDetection> findByApiRequestId(Long apiRequestId);

    List<ThreatDetection> findByRuleId(String ruleId);

    List<ThreatDetection> findByDetectionCategory(String detectionCategory);
}
