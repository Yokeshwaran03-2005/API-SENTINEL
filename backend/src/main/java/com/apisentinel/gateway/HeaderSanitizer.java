package com.apisentinel.gateway;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility for sanitizing HTTP headers and preventing credential leakage in logs and telemetry.
 */
public final class HeaderSanitizer {

    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>();

    static {
        SENSITIVE_HEADERS.add("authorization");
        SENSITIVE_HEADERS.add("proxy-authorization");
        SENSITIVE_HEADERS.add("cookie");
        SENSITIVE_HEADERS.add("set-cookie");
        SENSITIVE_HEADERS.add("x-api-key");
        SENSITIVE_HEADERS.add("api-key");
        SENSITIVE_HEADERS.add("apikey");
        SENSITIVE_HEADERS.add("x-auth-token");
        SENSITIVE_HEADERS.add("secret");
        SENSITIVE_HEADERS.add("password");
    }

    private HeaderSanitizer() {
    }

    /**
     * Extracts and sanitizes request headers into an unmodifiable map.
     * Sensitive tokens (Bearer, ApiKey, Cookies) are masked to prevent secret leakage.
     */
    public static Map<String, String> extractSanitizedHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames == null) {
            return Collections.emptyMap();
        }

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            String lowerName = headerName.toLowerCase();

            if (SENSITIVE_HEADERS.contains(lowerName)) {
                headers.put(headerName, maskSensitiveHeader(lowerName, headerValue));
            } else {
                headers.put(headerName, headerValue);
            }
        }

        return Collections.unmodifiableMap(headers);
    }

    /**
     * Masks sensitive headers. Completely redacts cookies and credentials,
     * while retaining partial token scheme for diagnostics.
     */
    public static String maskSensitiveHeader(String headerName, String value) {
        if (value == null || value.isBlank()) {
            return "[EMPTY]";
        }

        if ("cookie".equals(headerName) || "set-cookie".equals(headerName) || "password".equals(headerName)) {
            return "[MASKED]";
        }

        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = trimmed.substring(7).trim();
            if (token.length() > 8) {
                return "Bearer " + token.substring(0, 4) + "..." + token.substring(token.length() - 4) + " [MASKED]";
            }
            return "Bearer [MASKED]";
        }

        if (trimmed.regionMatches(true, 0, "Basic ", 0, 6)) {
            return "Basic [MASKED]";
        }

        if (trimmed.length() > 8) {
            return trimmed.substring(0, 3) + "..." + trimmed.substring(trimmed.length() - 3) + " [MASKED]";
        }

        return "[MASKED]";
    }
}
