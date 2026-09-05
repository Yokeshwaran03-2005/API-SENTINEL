package com.apisentinel.gateway;

import com.apisentinel.policy.SecurityDecision;

/**
 * Result returned by the Security Pipeline after evaluation of an ApiRequestContext.
 */
public record InspectionResult(
        RequestVerdict verdict,
        Double threatScore,
        String reason,
        SecurityDecision decision
) {
    public InspectionResult(RequestVerdict verdict, Double threatScore, String reason) {
        this(verdict, threatScore, reason, null);
    }

    public static InspectionResult allowed() {
        return new InspectionResult(RequestVerdict.ALLOWED, 0.0, "Traffic clean", null);
    }

    public static InspectionResult blocked(Double threatScore, String reason) {
        return new InspectionResult(RequestVerdict.BLOCKED, threatScore, reason, null);
    }

    public static InspectionResult rateLimited(String reason) {
        return new InspectionResult(RequestVerdict.RATE_LIMITED, 50.0, reason, null);
    }

    public static InspectionResult fromDecision(SecurityDecision decision) {
        RequestVerdict verdict = switch (decision.action()) {
            case BLOCK -> RequestVerdict.BLOCKED;
            case RATE_LIMIT -> RequestVerdict.RATE_LIMITED;
            default -> RequestVerdict.ALLOWED;
        };
        return new InspectionResult(verdict, (double) decision.threatScore(), decision.reason(), decision);
    }
}
