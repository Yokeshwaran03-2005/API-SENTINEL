package com.apisentinel.gateway;

/**
 * Authentication status evaluated for incoming API requests.
 */
public enum AuthStatus {
    AUTHENTICATED,
    ANONYMOUS,
    INVALID_CREDENTIALS,
    EXPIRED_TOKEN,
    API_KEY_VALID,
    API_KEY_INVALID
}
