package com.apisentinel.policy;

import com.apisentinel.scoring.RiskLevel;

/**
 * The final security decision produced by the Policy Engine.
 * Converts threat score assessment and policy rules into an actionable enforcement verdict.
 *
 * @param action enforcement action to take (ALLOW, MONITOR, WARN, RATE_LIMIT, BLOCK)
 * @param reason explainable rationale for the decision
 * @param threatScore the cumulative risk score (0 - 100)
 * @param riskLevel the classified risk tier (LOW, MEDIUM, HIGH, CRITICAL)
 */
public record SecurityDecision(
        PolicyAction action,
        String reason,
        int threatScore,
        RiskLevel riskLevel
) {
    public boolean isBlocked() {
        return action == PolicyAction.BLOCK;
    }

    public boolean isRateLimited() {
        return action == PolicyAction.RATE_LIMIT;
    }

    public boolean isAllowed() {
        return action == PolicyAction.ALLOW;
    }

    public boolean isMonitored() {
        return action == PolicyAction.MONITOR;
    }

    public boolean isWarning() {
        return action == PolicyAction.WARN;
    }

    public static SecurityDecision allow(String reason, int score, RiskLevel riskLevel) {
        return new SecurityDecision(PolicyAction.ALLOW, reason, score, riskLevel);
    }

    public static SecurityDecision monitor(String reason, int score, RiskLevel riskLevel) {
        return new SecurityDecision(PolicyAction.MONITOR, reason, score, riskLevel);
    }

    public static SecurityDecision warn(String reason, int score, RiskLevel riskLevel) {
        return new SecurityDecision(PolicyAction.WARN, reason, score, riskLevel);
    }

    public static SecurityDecision rateLimit(String reason, int score, RiskLevel riskLevel) {
        return new SecurityDecision(PolicyAction.RATE_LIMIT, reason, score, riskLevel);
    }

    public static SecurityDecision block(String reason, int score, RiskLevel riskLevel) {
        return new SecurityDecision(PolicyAction.BLOCK, reason, score, riskLevel);
    }
}
