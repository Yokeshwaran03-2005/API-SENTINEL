package com.apisentinel.events;

/**
 * Classification of threat vectors detected by API Sentinel.
 */
public enum ThreatType {
    SQL_INJECTION,
    XSS,
    BRUTE_FORCE,
    RATE_LIMIT_EXCEEDED,
    BOLA_IDOR,
    CREDENTIAL_STUFFING,
    SSRF,
    PATH_TRAVERSAL,
    DATA_EXFILTRATION,
    SUSPICIOUS_PAYLOAD,
    ANOMALOUS_BURST,
    UNKNOWN
}
