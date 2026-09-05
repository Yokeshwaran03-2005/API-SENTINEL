package com.apisentinel.policy;

/**
 * Type of source identifier blocked by the system.
 */
public enum SourceType {
    IP_ADDRESS,
    CIDR_BLOCK,
    USER_ID,
    API_KEY
}
