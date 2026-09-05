package com.apisentinel.gateway;

/**
 * Architectural interface separating Request Interception from Threat Detection,
 * Scoring, and Policy Decisions.
 *
 * Execution Pipeline:
 * [Request Interception Filter]
 *           ↓ (creates ApiRequestContext)
 * [SecurityPipeline]
 *      1. Threat Detection Engine (Phase 5)
 *      2. Dynamic Scoring Calculator (Phase 6)
 *      3. Policy Enforcement Engine (Phase 7)
 */
public interface SecurityPipeline {

    /**
     * Inspects incoming request context and returns security verdict.
     *
     * @param context the normalized, security-relevant request context
     * @return result with verdict, score, and decision reason
     */
    InspectionResult inspect(ApiRequestContext context);
}
