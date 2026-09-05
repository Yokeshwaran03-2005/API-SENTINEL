package com.apisentinel.scoring;

import com.apisentinel.events.ThreatType;

/**
 * Categorization of security threat vectors evaluated by API Sentinel.
 * Each category carries a deterministic default scoring weight.
 */
public enum ThreatCategory {
    AUTHENTICATION_ABUSE("Authentication Abuse", 20),
    INJECTION("Injection", 30),
    RATE_ABUSE("Rate Abuse", 20),
    ENUMERATION("Enumeration", 15),
    SENSITIVE_DATA_EXPOSURE("Sensitive Data Exposure", 15);

    private final String displayName;
    private final int defaultWeight;

    ThreatCategory(String displayName, int defaultWeight) {
        this.displayName = displayName;
        this.defaultWeight = defaultWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }

    /**
     * Maps a raw ThreatType to its corresponding canonical ThreatCategory.
     */
    public static ThreatCategory fromThreatType(ThreatType threatType) {
        if (threatType == null) {
            return null;
        }
        return switch (threatType) {
            case BRUTE_FORCE, CREDENTIAL_STUFFING -> AUTHENTICATION_ABUSE;
            case SQL_INJECTION, XSS, PATH_TRAVERSAL, SUSPICIOUS_PAYLOAD -> INJECTION;
            case RATE_LIMIT_EXCEEDED, ANOMALOUS_BURST -> RATE_ABUSE;
            case BOLA_IDOR -> ENUMERATION;
            case DATA_EXFILTRATION, SSRF -> SENSITIVE_DATA_EXPOSURE;
            default -> null;
        };
    }
}
