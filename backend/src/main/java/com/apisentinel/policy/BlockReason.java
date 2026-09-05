package com.apisentinel.policy;

/**
 * Reason for blocking a source.
 */
public enum BlockReason {
    REPEATED_ATTACKS,
    POLICY_VIOLATION,
    MANUAL_BLOCKLIST,
    CREDENTIAL_STUFFING,
    ANOMALOUS_VOLUME,
    HIGH_THREAT_SCORE
}
