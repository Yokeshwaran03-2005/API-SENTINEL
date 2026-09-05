package com.apisentinel.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configurable properties for the Threat Scoring Engine.
 * Supports runtime customization via application properties or programmatically.
 */
@Component
@ConfigurationProperties(prefix = "apisentinel.scoring")
public class ThreatScoringProperties {

    /**
     * Maximum aggregate threat score ceiling (default: 100).
     */
    private int maxScore = 100;

    /**
     * Threat category scoring weights (sum equals maxScore by default).
     */
    private Map<ThreatCategory, Integer> weights = new EnumMap<>(ThreatCategory.class);

    /**
     * Threshold boundaries.
     */
    private int lowMax = 29;
    private int mediumMin = 30;
    private int mediumMax = 59;
    private int highMin = 60;
    private int highMax = 79;
    private int criticalMin = 80;

    /**
     * Configurable recommended actions.
     */
    private RecommendedAction lowAction = RecommendedAction.ALLOW;
    private RecommendedAction mediumAction = RecommendedAction.MONITOR;
    private RecommendedAction highDefaultAction = RecommendedAction.WARN;
    private RecommendedAction highRateAbuseAction = RecommendedAction.RATE_LIMIT;
    private RecommendedAction criticalAction = RecommendedAction.BLOCK;

    public ThreatScoringProperties() {
        // Initialize default weights
        for (ThreatCategory category : ThreatCategory.values()) {
            weights.put(category, category.getDefaultWeight());
        }
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public Map<ThreatCategory, Integer> getWeights() {
        return weights;
    }

    public void setWeights(Map<ThreatCategory, Integer> weights) {
        if (weights != null) {
            this.weights = new EnumMap<>(weights);
        }
    }

    public int getWeightForCategory(ThreatCategory category) {
        if (category == null) {
            return 0;
        }
        return weights.getOrDefault(category, category.getDefaultWeight());
    }

    public void setWeightForCategory(ThreatCategory category, int weight) {
        weights.put(category, weight);
    }

    public int getLowMax() {
        return lowMax;
    }

    public void setLowMax(int lowMax) {
        this.lowMax = lowMax;
    }

    public int getMediumMin() {
        return mediumMin;
    }

    public void setMediumMin(int mediumMin) {
        this.mediumMin = mediumMin;
    }

    public int getMediumMax() {
        return mediumMax;
    }

    public void setMediumMax(int mediumMax) {
        this.mediumMax = mediumMax;
    }

    public int getHighMin() {
        return highMin;
    }

    public void setHighMin(int highMin) {
        this.highMin = highMin;
    }

    public int getHighMax() {
        return highMax;
    }

    public void setHighMax(int highMax) {
        this.highMax = highMax;
    }

    public int getCriticalMin() {
        return criticalMin;
    }

    public void setCriticalMin(int criticalMin) {
        this.criticalMin = criticalMin;
    }

    public RecommendedAction getLowAction() {
        return lowAction;
    }

    public void setLowAction(RecommendedAction lowAction) {
        this.lowAction = lowAction;
    }

    public RecommendedAction getMediumAction() {
        return mediumAction;
    }

    public void setMediumAction(RecommendedAction mediumAction) {
        this.mediumAction = mediumAction;
    }

    public RecommendedAction getHighDefaultAction() {
        return highDefaultAction;
    }

    public void setHighDefaultAction(RecommendedAction highDefaultAction) {
        this.highDefaultAction = highDefaultAction;
    }

    public RecommendedAction getHighRateAbuseAction() {
        return highRateAbuseAction;
    }

    public void setHighRateAbuseAction(RecommendedAction highRateAbuseAction) {
        this.highRateAbuseAction = highRateAbuseAction;
    }

    public RecommendedAction getCriticalAction() {
        return criticalAction;
    }

    public void setCriticalAction(RecommendedAction criticalAction) {
        this.criticalAction = criticalAction;
    }
}
