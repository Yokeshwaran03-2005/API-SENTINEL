package com.apisentinel.policy;

/**
 * Enforcement action decided by the Sentinel Policy Engine.
 * Supported actions:
 * - ALLOW: Permit request to reach downstream endpoint
 * - MONITOR: Allow request but flag for auditing and telemetry
 * - WARN: Allow request with warning audit and security response header
 * - RATE_LIMIT: Throttle or reject request with HTTP 429 Too Many Requests
 * - BLOCK: Terminate request immediately with HTTP 403 Forbidden
 * Legacy/Policy actions:
 * - CHALLENGE: Require identity or captcha verification
 * - LOG_AND_ALERT: High priority security notification
 */
public enum PolicyAction {
    ALLOW,
    MONITOR,
    WARN,
    RATE_LIMIT,
    BLOCK,
    CHALLENGE,
    LOG_AND_ALERT
}
