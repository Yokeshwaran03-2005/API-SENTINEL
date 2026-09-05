package com.apisentinel.scoring;

import com.apisentinel.detection.DetectionResult;

import java.util.Collections;
import java.util.List;

/**
 * Result of threat scoring evaluation for an API request.
 *
 * @param totalScore aggregated risk score between 0 and 100
 * @param riskLevel classified risk level (LOW, MEDIUM, HIGH, CRITICAL)
 * @param detectionResults list of positive threat detection findings evaluated
 * @param recommendedAction recommended mitigation action (ALLOW, MONITOR, WARN, RATE_LIMIT, BLOCK)
 * @param explanation clear, human-readable breakdown explaining how the score was calculated
 */
public record ThreatScoreResult(
        int totalScore,
        RiskLevel riskLevel,
        List<DetectionResult> detectionResults,
        RecommendedAction recommendedAction,
        String explanation
) {
    public ThreatScoreResult {
        detectionResults = detectionResults != null ? Collections.unmodifiableList(detectionResults) : Collections.emptyList();
    }
}
