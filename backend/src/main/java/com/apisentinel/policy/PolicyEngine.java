package com.apisentinel.policy;

import com.apisentinel.gateway.ApiRequestContext;
import com.apisentinel.scoring.ThreatScoreResult;

/**
 * Common contract for the Security Policy Engine.
 * Converts ThreatScoreResult into an authoritative SecurityDecision.
 */
public interface PolicyEngine {

    /**
     * Evaluates incoming request context and calculated threat score to make a policy decision.
     *
     * @param context normalized request context
     * @param scoreResult risk score result produced by ThreatScoringEngine
     * @return authoritative SecurityDecision
     */
    SecurityDecision evaluate(ApiRequestContext context, ThreatScoreResult scoreResult);

    /**
     * Checks if a client IP or source identifier is currently blocked.
     *
     * @param sourceValue IP address or identifier
     * @return true if currently blocked and not expired
     */
    boolean isSourceBlocked(String sourceValue);

    /**
     * Dynamically registers a source to be blocked for a temporary duration.
     *
     * @param sourceValue IP address or identifier
     * @param reason rationale for blocking
     * @param durationMinutes duration before block expires
     */
    void blockSource(String sourceValue, String reason, int durationMinutes);

    /**
     * Unblocks a source identifier.
     *
     * @param sourceValue IP address or identifier
     */
    void unblockSource(String sourceValue);
}
