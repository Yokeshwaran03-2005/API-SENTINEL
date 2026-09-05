package com.apisentinel.events.dto;

import com.apisentinel.events.MitigationAction;
import com.apisentinel.events.SecurityEvent;
import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;

import java.time.Instant;

/**
 * Clean data transfer object for SecurityEvent list and summary views.
 */
public record SecurityEventDto(
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
        Instant timestamp
) {
    public static SecurityEventDto fromEntity(SecurityEvent event) {
        if (event == null) return null;
        return new SecurityEventDto(
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
                event.getTimestamp()
        );
    }
}
