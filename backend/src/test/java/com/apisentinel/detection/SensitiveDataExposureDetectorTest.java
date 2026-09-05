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

class SensitiveDataExposureDetectorTest {

    private SensitiveDataExposureDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SensitiveDataExposureDetector(85.0);
    }

    @Test
    @DisplayName("Detects probes targeting internal diagnostic/environment endpoints (.env, actuator/env)")
    void testDiagnosticEndpointProbe() {
        ApiRequestContext context = new ApiRequestContext(
                "req-1", "GET", "/actuator/env", null, "198.51.100.1", Instant.now(),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.DATA_EXFILTRATION);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.HIGH);
        assertThat(result.reason()).contains("internal/diagnostic resource");
    }

    @Test
    @DisplayName("Detects sensitive field query attempts in query parameters (password_hash)")
    void testSensitiveFieldQueryInQueryParam() {
        ApiRequestContext context = new ApiRequestContext(
                "req-2", "GET", "/api/users", "fields=id,username,password_hash", "198.51.100.1", Instant.now(),
                Map.of(), Map.of("fields", "id,username,password_hash"), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.DATA_EXFILTRATION);
        assertThat(result.reason()).contains("password_hash");
    }

    @Test
    @DisplayName("Detects private key leakage in request body")
    void testPrivateKeyLeakageInBody() {
        String body = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...\n-----END RSA PRIVATE KEY-----";
        ApiRequestContext context = new ApiRequestContext(
                "req-3", "POST", "/api/keys/upload", null, "198.51.100.1", Instant.now(),
                Map.of(), Map.of(), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.DATA_EXFILTRATION);
        assertThat(result.severity()).isEqualTo(ThreatSeverity.CRITICAL);
        assertThat(result.reason()).contains("private key");
    }

    @Test
    @DisplayName("Detects Social Security Number (SSN) pattern in payload")
    void testSsnInPayload() {
        String body = "{\"name\":\"Alice\",\"ssn\":\"123-45-6789\"}";
        ApiRequestContext context = new ApiRequestContext(
                "req-4", "POST", "/api/submit", null, "198.51.100.1", Instant.now(),
                Map.of(), Map.of(), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isTrue();
        assertThat(result.threatType()).isEqualTo(ThreatType.DATA_EXFILTRATION);
        assertThat(result.reason()).contains("Social Security Number");
    }

    @Test
    @DisplayName("Clean legitimate API request passes without triggering detector")
    void testCleanDataRequest() {
        ApiRequestContext context = new ApiRequestContext(
                "req-clean", "GET", "/api/v1/orders", "page=1&size=20", "198.51.100.1", Instant.now(),
                Map.of(), Map.of("page", "1", "size", "20"), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        DetectionResult result = detector.detect(context);
        assertThat(result.detected()).isFalse();
    }
}
