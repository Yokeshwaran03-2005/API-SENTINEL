package com.apisentinel.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SecurityEvent entity.
 */
@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long>, JpaSpecificationExecutor<SecurityEvent> {

    Optional<SecurityEvent> findByEventId(String eventId);

    Page<SecurityEvent> findBySeverity(ThreatSeverity severity, Pageable pageable);

    Page<SecurityEvent> findByThreatType(ThreatType threatType, Pageable pageable);

    List<SecurityEvent> findTop10ByOrderByTimestampDesc();

    long countBySeverity(ThreatSeverity severity);

    long countByActionTaken(MitigationAction actionTaken);

    List<SecurityEvent> findByTimestampBetween(Instant start, Instant end);
}
