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

class InjectionDetectorTest {

    private InjectionDetector detector;

    @BeforeEach
    void setUp() {
        detector = new InjectionDetector(90.0);
    }

    @Test
    @DisplayName("Detects boolean-based SQL injection in query parameters")
    void testBooleanSqlInjectionInQueryParam() {
        ApiRequestContext context = new ApiRequestContext(
                "req-1", "GET", "/api/v1/users", "id=1' OR 1=1", "192.168.1.1", Instant.now(),
                Map.of(), Map.of("id", "1' OR 1=1"), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.SQL_INJECTION);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.CRITICAL);
        assertThat(result.reason()).contains("Boolean-based SQL injection");
    }

    @Test
    @DisplayName("Detects Union-based SQL injection in request body")
    void testUnionSelectInBody() {
        String body = "{\"search\":\"shoes' UNION SELECT null, username, password FROM users --\"}";
        ApiRequestContext context = new ApiRequestContext(
                "req-2", "POST", "/api/v1/items", null, "192.168.1.1", Instant.now(),
                Map.of(), Map.of(), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.SQL_INJECTION);
        assertThat(result.reason()).contains("Union-based SQL injection");
    }

    @Test
    @DisplayName("Detects destructive SQL statements (DROP TABLE)")
    void testDestructiveSqlInjection() {
        ApiRequestContext context = new ApiRequestContext(
                "req-3", "POST", "/api/v1/comments", null, "192.168.1.1", Instant.now(),
                Map.of(), Map.of(), "; DROP TABLE users", 20,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.SQL_INJECTION);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Detects Path Traversal sequence in URI path")
    void testPathTraversal() {
        ApiRequestContext context = new ApiRequestContext(
                "req-4", "GET", "/api/v1/files/../../etc/passwd", null, "192.168.1.1", Instant.now(),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.PATH_TRAVERSAL);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.HIGH);
    }

    @Test
    @DisplayName("Detects XSS script tags in payload")
    void testXssDetection() {
        String body = "{\"comment\":\"<script>alert('pwned')</script>\"}";
        ApiRequestContext context = new ApiRequestContext(
                "req-5", "POST", "/api/v1/reviews", null, "192.168.1.1", Instant.now(),
                Map.of(), Map.of(), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.XSS);
    }

    @Test
    @DisplayName("Clean payload passes without false positives")
    void testCleanPayload() {
        String body = "{\"name\":\"John Doe\",\"email\":\"john@example.com\",\"city\":\"New York\"}";
        ApiRequestContext context = new ApiRequestContext(
                "req-clean", "POST", "/api/v1/users", null, "192.168.1.1", Instant.now(),
                Map.of(), Map.of(), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isFalse();
    }
}
