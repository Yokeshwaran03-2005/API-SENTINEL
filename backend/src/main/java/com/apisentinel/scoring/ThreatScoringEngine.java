package com.apisentinel.scoring;

import com.apisentinel.detection.DetectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Modular Threat Scoring Engine.
 * Evaluates individual detection results, aggregates category weights,
 * maps cumulative score to risk thresholds, and derives explainable recommended actions.
 */
@Service
public class ThreatScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(ThreatScoringEngine.class);

    private final ThreatScoringProperties properties;

    public ThreatScoringEngine(ThreatScoringProperties properties) {
        this.properties = properties != null ? properties : new ThreatScoringProperties();
    }

    /**
     * Scores a collection of detection results produced during request evaluation.
     *
     * @param detectionResults list of detection findings
     * @return ThreatScoreResult containing total score, risk level, action, and explanation
     */
    public ThreatScoreResult score(List<DetectionResult> detectionResults) {
        if (detectionResults == null || detectionResults.isEmpty()) {
            return buildScoreResult(0, Collections.emptyMap(), Collections.emptyList(), RiskLevel.LOW, properties.getLowAction());
        }

        // Filter for positive detections
        List<DetectionResult> activeDetections = detectionResults.stream()
                .filter(DetectionResult::detected)
                .toList();

        if (activeDetections.isEmpty()) {
            return buildScoreResult(0, Collections.emptyMap(), Collections.emptyList(), RiskLevel.LOW, properties.getLowAction());
        }

        // Map category contributions (deduplicating multiple detections per category up to category weight)
        Map<ThreatCategory, Integer> categoryContributions = new LinkedHashMap<>();
        for (DetectionResult detection : activeDetections) {
            ThreatCategory category = detection.getCategory();
            if (category != null && !categoryContributions.containsKey(category)) {
                int weight = properties.getWeightForCategory(category);
                categoryContributions.put(category, weight);
            }
        }

        // Sum contributions capped at configured maxScore (default 100)
        int rawScore = categoryContributions.values().stream().mapToInt(Integer::intValue).sum();
        int totalScore = Math.min(rawScore, properties.getMaxScore());

        // Determine risk level based on thresholds
        RiskLevel riskLevel = evaluateRiskLevel(totalScore);

        // Derive recommended action
        RecommendedAction recommendedAction = resolveAction(riskLevel, categoryContributions.keySet());

        ThreatScoreResult result = buildScoreResult(totalScore, categoryContributions, activeDetections, riskLevel, recommendedAction);

        log.debug("Evaluated threat score: {} [{}] -> Action: {}", totalScore, riskLevel, recommendedAction);
        return result;
    }

    /**
     * Evaluates the risk level corresponding to a total numeric score against configured thresholds.
     */
    public RiskLevel evaluateRiskLevel(int score) {
        if (score >= properties.getCriticalMin()) {
            return RiskLevel.CRITICAL;
        } else if (score >= properties.getHighMin()) {
            return RiskLevel.HIGH;
        } else if (score >= properties.getMediumMin()) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    /**
     * Resolves the recommended security action based on risk level and detected categories.
     */
    public RecommendedAction resolveAction(RiskLevel riskLevel, Set<ThreatCategory> triggeredCategories) {
        return switch (riskLevel) {
            case LOW -> properties.getLowAction();
            case MEDIUM -> properties.getMediumAction();
            case HIGH -> (triggeredCategories != null && triggeredCategories.contains(ThreatCategory.RATE_ABUSE))
                    ? properties.getHighRateAbuseAction()
                    : properties.getHighDefaultAction();
            case CRITICAL -> properties.getCriticalAction();
        };
    }

    /**
     * Generates a deterministic, human-readable breakdown explaining why the score was produced.
     */
    public String generateExplanation(int totalScore, Map<ThreatCategory, Integer> contributions, RiskLevel riskLevel, RecommendedAction action) {
        StringBuilder explanation = new StringBuilder();

        if (contributions == null || contributions.isEmpty()) {
            explanation.append("No threats detected.\n\n");
        } else {
            for (Map.Entry<ThreatCategory, Integer> entry : contributions.entrySet()) {
                explanation.append(entry.getKey().getDisplayName())
                        .append(" = ")
                        .append(entry.getValue())
                        .append("\n");
            }
            explanation.append("\n");
        }

        explanation.append("Total = ").append(totalScore).append("\n\n");
        explanation.append("Risk = ").append(riskLevel).append("\n\n");
        explanation.append("Recommended action = ").append(action);

        return explanation.toString();
    }

    private ThreatScoreResult buildScoreResult(
            int totalScore,
            Map<ThreatCategory, Integer> contributions,
            List<DetectionResult> detections,
            RiskLevel riskLevel,
            RecommendedAction action
    ) {
        String explanation = generateExplanation(totalScore, contributions, riskLevel, action);
        return new ThreatScoreResult(totalScore, riskLevel, detections, action, explanation);
    }

    public ThreatScoringProperties getProperties() {
        return properties;
    }
}
