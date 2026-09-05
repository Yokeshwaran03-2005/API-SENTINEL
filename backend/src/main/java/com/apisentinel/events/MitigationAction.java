package com.apisentinel.events;

/**
 * Enforcement or mitigation action triggered in response to a security threat.
 */
public enum MitigationAction {
    ALLOWED,
    BLOCKED,
    RATE_LIMITED,
    CAPTCHA_CHALLENGED,
    IP_BANNED,
    ALERT_ONLY
}
