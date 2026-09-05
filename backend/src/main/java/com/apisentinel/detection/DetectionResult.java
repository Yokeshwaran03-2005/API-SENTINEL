package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.scoring.ThreatCategory;

/**
 * Deterministic detection finding produced by a ThreatDetector.
 */
public record DetectionResult(
        boolean detected,
        ThreatType threatType,
        ThreatSeverity severity,
        double scoreContribution,
        String reason,
        String evidence
) {
    public static DetectionResult clean() {
        return new DetectionResult(false, null, null, 0.0, null, null);
    }

    public static DetectionResult detected(
            ThreatType threatType,
            ThreatSeverity severity,
            double scoreContribution,
            String reason,
            String evidence
    ) {
        return new DetectionResult(true, threatType, severity, scoreContribution, reason, evidence);
    }

    /**
     * Resolves the canonical ThreatCategory for this detection result.
     */
    public ThreatCategory getCategory() {
        return ThreatCategory.fromThreatType(threatType);
    }
}

