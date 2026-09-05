package com.apisentinel.scoring;

/**
 * Recommended enforcement or mitigation actions based on assessed risk level and detected threat vectors.
 * - LOW -> ALLOW
 * - MEDIUM -> MONITOR
 * - HIGH -> WARN or RATE_LIMIT
 * - CRITICAL -> BLOCK
 */
public enum RecommendedAction {
    ALLOW,
    MONITOR,
    WARN,
    RATE_LIMIT,
    BLOCK
}
