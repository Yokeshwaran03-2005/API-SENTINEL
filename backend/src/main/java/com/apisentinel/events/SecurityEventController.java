package com.apisentinel.events;

import com.apisentinel.common.dto.PagedResponse;
import com.apisentinel.events.dto.SecurityEventDetailDto;
import com.apisentinel.events.dto.SecurityEventDto;
import com.apisentinel.events.dto.SecurityStatisticsDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST Controller exposing SecurityEvent listings, details, and aggregated dashboard statistics.
 */
@RestController
@RequestMapping("/api/security")
public class SecurityEventController {

    private final SecurityEventService securityEventService;

    public SecurityEventController(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    /**
     * GET /api/security/events
     * Paginated and filterable security event audit log.
     */
    @GetMapping("/events")
    public ResponseEntity<PagedResponse<SecurityEventDto>> getSecurityEvents(
            @RequestParam(required = false) ThreatSeverity severity,
            @RequestParam(required = false) ThreatType threatType,
            @RequestParam(required = false) MitigationAction action,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable
    ) {
        PagedResponse<SecurityEventDto> response = securityEventService.findEvents(
                severity, threatType, action, endpoint, source, startDate, endDate, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/security/events/{id}
     * Full security event details with fine-grained rule detections.
     */
    @GetMapping("/events/{id}")
    public ResponseEntity<SecurityEventDetailDto> getSecurityEventById(@PathVariable String id) {
        SecurityEventDetailDto detail = securityEventService.getEventById(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * GET /api/security/statistics
     * Key dashboard metrics and aggregations.
     */
    @GetMapping("/statistics")
    public ResponseEntity<SecurityStatisticsDto> getSecurityStatistics() {
        SecurityStatisticsDto stats = securityEventService.getSecurityStatistics();
        return ResponseEntity.ok(stats);
    }
}
