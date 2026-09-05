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

class RateAbuseDetectorTest {

    private RateAbuseDetector detector;

    @BeforeEach
    void setUp() {
        // maxRequests = 10, windowSeconds = 60, burstThreshold = 5, burstSeconds = 2, score = 65.0
        detector = new RateAbuseDetector(10, 60, 5, 2, 65.0);
        detector.resetTracker();
    }

    @Test
    @DisplayName("Detects rapid burst flooding exceeding burst threshold")
    void testBurstFloodDetection() {
        String ip = "198.51.100.12";
        Instant now = Instant.now();

        // 5 requests within 1s -> within burst threshold
        for (int i = 1; i <= 5; i++) {
            ApiRequestContext context = new ApiRequestContext(
                    "req-" + i, "GET", "/api/data", null, ip, now.plusMillis(i * 100),
                    Map.of(), Map.of(), "", 0,
                    AuthStatus.ANONYMOUS, "NONE", null, "agent"
            );
            DetectionResult result = detector.detect(context);
            assertThat(result.detected()).isFalse();
        }

        // 6th request within 1s -> triggers burst detection
        ApiRequestContext sixth = new ApiRequestContext(
                "req-6", "GET", "/api/data", null, ip, now.plusMillis(600),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );
        DetectionResult result = detector.detect(sixth);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.ANOMALOUS_BURST);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.HIGH);
        assertThat(result.reason()).contains("Volumetric burst detected");
    }

    @Test
    @DisplayName("Detects sustained volume exceeding max requests in window")
    void testSustainedRateLimitExceeded() {
        String ip = "198.51.100.33";
        Instant now = Instant.now();

        // Send 10 requests spaced out over 20s (no burst, but accumulates in 60s window)
        for (int i = 1; i <= 10; i++) {
            ApiRequestContext context = new ApiRequestContext(
                    "req-" + i, "GET", "/api/data", null, ip, now.plusSeconds(i * 2),
                    Map.of(), Map.of(), "", 0,
                    AuthStatus.ANONYMOUS, "NONE", null, "agent"
            );
            DetectionResult result = detector.detect(context);
            assertThat(result.detected()).isFalse();
        }

        // 11th request exceeds max limit of 10
        ApiRequestContext eleventh = new ApiRequestContext(
                "req-11", "GET", "/api/data", null, ip, now.plusSeconds(25),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );
        DetectionResult result = detector.detect(eleventh);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.RATE_LIMIT_EXCEEDED);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.MEDIUM);
        assertThat(result.reason()).contains("Exceeded sustained rate quota");
    }

    @Test
    @DisplayName("Requests below threshold are clean")
    void testCleanRateTraffic() {
        ApiRequestContext context = new ApiRequestContext(
                "req-single", "GET", "/api/status", null, "198.51.100.99", Instant.now(),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isFalse();
    }
}
