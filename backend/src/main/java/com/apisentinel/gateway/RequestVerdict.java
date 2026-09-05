package com.apisentinel.gateway;

/**
 * Gateway mitigation verdict applied to an incoming API request.
 */
public enum RequestVerdict {
    ALLOWED,
    BLOCKED,
    CHALLENGED,
    RATE_LIMITED
}
