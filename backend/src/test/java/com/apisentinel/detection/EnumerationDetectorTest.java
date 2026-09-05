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

class EnumerationDetectorTest {

    private EnumerationDetector detector;

    @BeforeEach
    void setUp() {
        // threshold = 4 distinct IDs, window = 60s, score = 80.0
        detector = new EnumerationDetector(4, 60, 80.0);
        detector.resetTracker();
    }

    @Test
    @DisplayName("Detects sequential resource identifier enumeration (BOLA/IDOR probing)")
    void testSequentialIdEnumeration() {
        String ip = "192.0.2.100";
        Instant now = Instant.now();

        // Access 4 different IDs: /api/users/1, /api/users/2, /api/users/3, /api/users/4 (threshold is 4)
        for (int i = 1; i <= 4; i++) {
            ApiRequestContext context = new ApiRequestContext(
                    "req-" + i, "GET", "/api/users/" + i, null, ip, now.plusSeconds(i),
                    Map.of(), Map.of(), "", 0,
                    AuthStatus.ANONYMOUS, "NONE", null, "agent"
            );
            DetectionResult result = detector.detect(context);
            assertThat(result.detected()).isFalse();
        }

        // 5th distinct ID: /api/users/5 -> triggers detection
        ApiRequestContext fifth = new ApiRequestContext(
                "req-5", "GET", "/api/users/5", null, ip, now.plusSeconds(5),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(fifth);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.BOLA_IDOR);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.HIGH);
        assertThat(result.reason()).contains("Resource enumeration probe detected");
        assertThat(result.evidence()).contains("Enumerated identifiers");
    }

    @Test
    @DisplayName("Repeated access to the same single ID does not trigger enumeration")
    void testRepeatedAccessToSameIdNotTriggered() {
        String ip = "192.0.2.101";
        Instant now = Instant.now();

        // Access same ID 10 times
        for (int i = 1; i <= 10; i++) {
            ApiRequestContext context = new ApiRequestContext(
                    "req-" + i, "GET", "/api/users/42", null, ip, now.plusSeconds(i),
                    Map.of(), Map.of(), "", 0,
                    AuthStatus.ANONYMOUS, "NONE", null, "agent"
            );
            DetectionResult result = detector.detect(context);
            assertThat(result.detected()).isFalse();
        }
    }

    @Test
    @DisplayName("Static paths without identifier segment are not evaluated")
    void testStaticPathsIgnored() {
        ApiRequestContext context = new ApiRequestContext(
                "req-static", "GET", "/api/dashboard/stats", null, "192.0.2.102", Instant.now(),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isFalse();
    }
}
