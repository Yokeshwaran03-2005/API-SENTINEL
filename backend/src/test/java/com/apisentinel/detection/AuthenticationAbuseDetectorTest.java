package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequestContext;
import com.apisentinel.gateway.AuthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationAbuseDetectorTest {

    private AuthenticationAbuseDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AuthenticationAbuseDetector(3, 60, 80.0);
        detector.resetTracker();
    }

    @Test
    @DisplayName("Detects suspicious dummy token in Authorization header")
    void testSuspiciousTokenDetection() {
        ApiRequestContext context = new ApiRequestContext(
                "req-1", "GET", "/api/v1/profile", null, "192.168.1.1", Instant.now(),
                Map.of("Authorization", "Bearer null"), Map.of(), "", 0,
                AuthStatus.AUTHENTICATED, "BEARER", "bearer:null", "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.HIGH);
        assertThat(result.reason()).contains("Suspicious or dummy authorization token");
    }

    @Test
    @DisplayName("Detects dictionary credentials in authentication payload")
    void testDictionaryCredentialsInPayload() {
        String body = "{\"username\":\"admin\",\"password\":\"password\"}";
        ApiRequestContext context = new ApiRequestContext(
                "req-2", "POST", "/api/v1/auth/login", null, "192.168.1.1", Instant.now(),
                Map.of(), Map.of(), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", "client", "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.CREDENTIAL_STUFFING);
        assertThat(result.reason()).contains("Default or dictionary password signature");
    }

    @Test
    @DisplayName("Detects repeated rapid login requests exceeding failure threshold")
    void testBruteForceThresholdExceeded() {
        String ip = "203.0.113.50";
        Instant now = Instant.now();

        // 3 requests within threshold -> not yet detected
        for (int i = 1; i <= 3; i++) {
            ApiRequestContext context = new ApiRequestContext(
                    "req-" + i, "POST", "/api/v1/auth/login", null, ip, now.plusSeconds(i),
                    Map.of(), Map.of(), "{\"username\":\"user" + i + "\"}", 30,
                    AuthStatus.ANONYMOUS, "NONE", null, "agent"
            );
            DetectionResult result = detector.detect(context);
            assertThat(result.detected()).isFalse();
        }

        // 4th request exceeds threshold of 3
        ApiRequestContext fourth = new ApiRequestContext(
                "req-4", "POST", "/api/v1/auth/login", null, ip, now.plusSeconds(4),
                Map.of(), Map.of(), "{\"username\":\"user4\"}", 30,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );
        DetectionResult result = detector.detect(fourth);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.BRUTE_FORCE);
        assertThat(result.reason()).contains("Exceeded authentication attempt threshold");
    }

    @Test
    @DisplayName("Clean request to non-auth endpoint returns clean result")
    void testCleanRequest() {
        ApiRequestContext context = new ApiRequestContext(
                "req-clean", "GET", "/api/v1/products", null, "192.168.1.1", Instant.now(),
                Map.of("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.validtoken.signature"),
                Map.of(), "", 0,
                AuthStatus.AUTHENTICATED, "BEARER", "user1", "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isFalse();
    }
}
