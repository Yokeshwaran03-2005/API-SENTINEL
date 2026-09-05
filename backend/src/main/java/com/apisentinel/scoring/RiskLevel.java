package com.apisentinel.scoring;

/**
 * Risk classification levels determined by cumulative threat score thresholds.
 * Thresholds:
 * - LOW: 0 - 29
 * - MEDIUM: 30 - 59
 * - HIGH: 60 - 79
 * - CRITICAL: 80 - 100
 */
public enum RiskLevel {
    LOW(0, 29),
    MEDIUM(30, 59),
    HIGH(60, 79),
    CRITICAL(80, 100);

    private final int minScore;
    private final int maxScore;

    RiskLevel(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    /**
     * Resolves the default RiskLevel corresponding to the given numeric score.
     */
    public static RiskLevel fromScore(int score) {
        if (score >= 80) {
            return CRITICAL;
        } else if (score >= 60) {
            return HIGH;
        } else if (score >= 30) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
