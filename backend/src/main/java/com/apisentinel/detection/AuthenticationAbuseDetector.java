package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Deterministic detector for authentication abuse, brute-force, and credential stuffing.
 * Evaluates suspicious tokens, credential dictionary probes, and rapid repetitive auth endpoint hits.
 */
@Component
public class AuthenticationAbuseDetector implements ThreatDetector {

    private static final Set<String> AUTH_PATH_KEYWORDS = Set.of(
            "/auth", "/login", "/signin", "/token", "/authenticate", "/oauth"
    );

    private static final Pattern SUSPICIOUS_TOKEN_PATTERN = Pattern.compile(
            "Bearer\\s+(null|undefined|test|dummy|admin|none|'|\"|<script)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DICTIONARY_CREDENTIAL_PATTERN = Pattern.compile(
            "\"(password|pwd|pass)\"\\s*:\\s*\"(admin|password|123456|root|guest|toor|12345678|qwerty)\"",
            Pattern.CASE_INSENSITIVE
    );

    private final int failureThreshold;
    private final int windowSeconds;
    private final double scoreContribution;

    // Sliding window of auth access timestamps per source IP
    private final Map<String, Deque<Instant>> authAttemptTracker = new ConcurrentHashMap<>();

    public AuthenticationAbuseDetector(
            @Value("${sentinel.detection.auth.failure-threshold:5}") int failureThreshold,
            @Value("${sentinel.detection.auth.window-seconds:60}") int windowSeconds,
            @Value("${sentinel.detection.auth.score:80.0}") double scoreContribution
    ) {
        this.failureThreshold = failureThreshold;
        this.windowSeconds = windowSeconds;
        this.scoreContribution = scoreContribution;
    }

    @Override
    public String getDetectorName() {
        return "AuthenticationAbuseDetector";
    }

    @Override
    public DetectionResult detect(ApiRequestContext context) {
        // 1. Check for obviously malformed or suspicious tokens
        for (Map.Entry<String, String> header : context.getHeaders().entrySet()) {
            if ("authorization".equalsIgnoreCase(header.getKey())) {
                String value = header.getValue();
                if (value != null && SUSPICIOUS_TOKEN_PATTERN.matcher(value).find()) {
                    return DetectionResult.detected(
                            ThreatType.CREDENTIAL_STUFFING,
                            ThreatSeverity.HIGH,
                            scoreContribution,
                            "Suspicious or dummy authorization token detected",
                            "Header Authorization matched signature: " + value
                    );
                }
            }
        }

        // 2. Check for dictionary credential stuffing signatures in body
        if (context.hasBody() && isAuthEndpoint(context.getPath())) {
            if (DICTIONARY_CREDENTIAL_PATTERN.matcher(context.getBody()).find()) {
                return DetectionResult.detected(
                        ThreatType.CREDENTIAL_STUFFING,
                        ThreatSeverity.HIGH,
                        scoreContribution,
                        "Default or dictionary password signature identified in authentication payload",
                        "Matched common brute-force credential dictionary pattern"
                );
            }
        }

        // 3. Check for rapid repeated authentication endpoint accesses (Brute-Force tracking)
        if (isAuthEndpoint(context.getPath())) {
            String sourceKey = context.getSourceIp();
            Instant now = context.getTimestamp();
            Instant cutoff = now.minusSeconds(windowSeconds);

            Deque<Instant> attempts = authAttemptTracker.computeIfAbsent(sourceKey, k -> new ArrayDeque<>());

            synchronized (attempts) {
                // Evict expired timestamps
                while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                    attempts.pollFirst();
                }

                attempts.addLast(now);

                if (attempts.size() > failureThreshold) {
                    return DetectionResult.detected(
                            ThreatType.BRUTE_FORCE,
                            ThreatSeverity.HIGH,
                            scoreContribution,
                            String.format("Exceeded authentication attempt threshold: %d attempts within %ds",
                                    attempts.size(), windowSeconds),
                            String.format("Source %s targeted auth endpoint %s repeatedly", sourceKey, context.getPath())
                    );
                }
            }
        }

        return DetectionResult.clean();
    }

    private boolean isAuthEndpoint(String path) {
        if (path == null) {
            return false;
        }
        String lower = path.toLowerCase();
        for (String keyword : AUTH_PATH_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Helper for testing to reset tracker state
    public void resetTracker() {
        authAttemptTracker.clear();
    }
}
