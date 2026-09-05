package com.apisentinel.policy;

/**
 * Types of security policies enforceable by API Sentinel.
 */
public enum PolicyType {
    RATE_LIMIT,
    IP_WHITELIST,
    IP_BLACKLIST,
    THREAT_THRESHOLD,
    PAYLOAD_INSPECTION,
    GEO_RESTRICTION
}
