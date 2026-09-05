package com.apisentinel.events.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated metrics and security statistics for frontend dashboard display.
 */
public record SecurityStatisticsDto(
        long totalRequests,
        long blockedRequests,
        long rateLimitedRequests,
        long allowedRequests,
        long totalSecurityEvents,
        double averageThreatScore,
        Map<String, Long> eventsBySeverity,
        Map<String, Long> eventsByThreatType,
        long activeBlockedSources,
        List<SecurityEventDto> recentEvents
) {}
