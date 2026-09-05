package com.apisentinel.events;

import com.apisentinel.common.dto.PagedResponse;
import com.apisentinel.common.exception.ResourceNotFoundException;
import com.apisentinel.events.dto.SecurityEventDetailDto;
import com.apisentinel.events.dto.SecurityEventDto;
import com.apisentinel.events.dto.SecurityStatisticsDto;
import com.apisentinel.gateway.ApiRequestRepository;
import com.apisentinel.gateway.RequestVerdict;
import com.apisentinel.policy.BlockedSourceRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service orchestrating SecurityEvent operations, querying, and statistics calculation.
 */
@Service
@Transactional(readOnly = true)
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;
    private final ApiRequestRepository apiRequestRepository;
    private final BlockedSourceRepository blockedSourceRepository;

    public SecurityEventService(
            SecurityEventRepository securityEventRepository,
            ApiRequestRepository apiRequestRepository,
            BlockedSourceRepository blockedSourceRepository
    ) {
        this.securityEventRepository = securityEventRepository;
        this.apiRequestRepository = apiRequestRepository;
        this.blockedSourceRepository = blockedSourceRepository;
    }

    /**
     * Queries security events matching optional multi-criteria filters with pagination.
     */
    public PagedResponse<SecurityEventDto> findEvents(
            ThreatSeverity severity,
            ThreatType threatType,
            MitigationAction action,
            String endpoint,
            String source,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        Specification<SecurityEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (threatType != null) {
                predicates.add(cb.equal(root.get("threatType"), threatType));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("actionTaken"), action));
            }
            if (endpoint != null && !endpoint.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("endpoint")), "%" + endpoint.trim().toLowerCase() + "%"));
            }
            if (source != null && !source.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("source")), "%" + source.trim().toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<SecurityEventDto> dtoPage = securityEventRepository.findAll(spec, sortedPageable)
                .map(SecurityEventDto::fromEntity);

        return PagedResponse.fromPage(dtoPage);
    }

    /**
     * Retrieves an event by its numeric ID or UUID eventId.
     */
    public SecurityEventDetailDto getEventById(String idOrEventId) {
        SecurityEvent event = null;

        // Try numeric ID lookup first
        try {
            Long numericId = Long.parseLong(idOrEventId);
            event = securityEventRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException ignored) {
            // Not numeric, fallback to eventId string
        }

        if (event == null) {
            event = securityEventRepository.findByEventId(idOrEventId)
                    .orElseThrow(() -> new ResourceNotFoundException("SecurityEvent", idOrEventId));
        }

        return SecurityEventDetailDto.fromEntity(event);
    }

    /**
     * Aggregates real-time security statistics for dashboard analytics.
     */
    public SecurityStatisticsDto getSecurityStatistics() {
        long totalRequests = apiRequestRepository != null ? apiRequestRepository.count() : 0;
        long blockedRequests = apiRequestRepository != null ? apiRequestRepository.countByVerdict(RequestVerdict.BLOCKED) : 0;
        long rateLimitedRequests = apiRequestRepository != null ? apiRequestRepository.countByVerdict(RequestVerdict.RATE_LIMITED) : 0;
        long allowedRequests = apiRequestRepository != null ? apiRequestRepository.countByVerdict(RequestVerdict.ALLOWED) : 0;
        long totalSecurityEvents = securityEventRepository.count();

        // Calculate average threat score across all security events
        List<SecurityEvent> allEvents = securityEventRepository.findAll();
        double averageThreatScore = allEvents.isEmpty() ? 0.0 :
                allEvents.stream().mapToDouble(e -> e.getThreatScore() != null ? e.getThreatScore() : 0.0).average().orElse(0.0);
        // Round to 1 decimal place
        averageThreatScore = Math.round(averageThreatScore * 10.0) / 10.0;

        // Group events by severity
        Map<String, Long> eventsBySeverity = new LinkedHashMap<>();
        for (ThreatSeverity severity : ThreatSeverity.values()) {
            long count = securityEventRepository.countBySeverity(severity);
            eventsBySeverity.put(severity.name(), count);
        }

        // Group events by threat type
        Map<String, Long> eventsByThreatType = new LinkedHashMap<>();
        for (ThreatType threatType : ThreatType.values()) {
            long count = allEvents.stream().filter(e -> e.getThreatType() == threatType).count();
            if (count > 0) {
                eventsByThreatType.put(threatType.name(), count);
            }
        }

        // Active blocked sources
        long activeBlocked = blockedSourceRepository != null ? blockedSourceRepository.findByIsActiveTrue().size() : 0;

        // Top 10 recent events
        List<SecurityEventDto> recentEvents = securityEventRepository.findTop10ByOrderByTimestampDesc().stream()
                .map(SecurityEventDto::fromEntity)
                .toList();

        return new SecurityStatisticsDto(
                totalRequests,
                blockedRequests,
                rateLimitedRequests,
                allowedRequests,
                totalSecurityEvents,
                averageThreatScore,
                eventsBySeverity,
                eventsByThreatType,
                activeBlocked,
                recentEvents
        );
    }
}
