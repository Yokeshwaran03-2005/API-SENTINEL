package com.apisentinel.events.dto;

import com.apisentinel.events.MitigationAction;
import com.apisentinel.events.SecurityEvent;
import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Detailed data transfer object for SecurityEvent with associated fine-grained rule detections.
 */
public record SecurityEventDetailDto(
        Long id,
        String eventId,
        ThreatType threatType,
        ThreatSeverity severity,
        Double threatScore,
        String reason,
        MitigationAction actionTaken,
        String endpoint,
        String source,
        String evidence,
        String apiRequestId,
        List<ThreatDetectionDto> detections,
        Instant timestamp
) {
    public static SecurityEventDetailDto fromEntity(SecurityEvent event) {
        if (event == null) return null;

        List<ThreatDetectionDto> detectionDtos = event.getDetections() != null
                ? event.getDetections().stream().map(ThreatDetectionDto::fromEntity).toList()
                : Collections.emptyList();

        String apiReqId = event.getApiRequest() != null ? event.getApiRequest().getRequestId() : null;

        return new SecurityEventDetailDto(
                event.getId(),
                event.getEventId(),
                event.getThreatType(),
                event.getSeverity(),
                event.getThreatScore(),
                event.getReason(),
                event.getActionTaken(),
                event.getEndpoint(),
                event.getSource(),
                event.getEvidence(),
                apiReqId,
                detectionDtos,
                event.getTimestamp()
        );
    }
}
