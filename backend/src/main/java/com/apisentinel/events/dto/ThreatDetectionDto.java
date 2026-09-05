package com.apisentinel.events.dto;

import com.apisentinel.detection.ThreatDetection;

import java.time.Instant;

/**
 * Data transfer object for individual threat detection details.
 */
public record ThreatDetectionDto(
        Long id,
        String ruleId,
        String ruleName,
        String detectionCategory,
        Double confidenceScore,
        String matchedPattern,
        String payloadLocation,
        String payloadSnippet,
        Instant timestamp
) {
    public static ThreatDetectionDto fromEntity(ThreatDetection detection) {
        if (detection == null) return null;
        return new ThreatDetectionDto(
                detection.getId(),
                detection.getRuleId(),
                detection.getRuleName(),
                detection.getDetectionCategory(),
                detection.getConfidenceScore(),
                detection.getMatchedPattern(),
                detection.getPayloadLocation(),
                detection.getPayloadSnippet(),
                detection.getTimestamp()
        );
    }
}
