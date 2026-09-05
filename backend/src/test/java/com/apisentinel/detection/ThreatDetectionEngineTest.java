package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequestContext;
import com.apisentinel.gateway.AuthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThreatDetectionEngineTest {

    private ThreatDetectionEngine engine;

    @BeforeEach
    void setUp() {
        List<ThreatDetector> detectors = List.of(
                new AuthenticationAbuseDetector(5, 60, 80.0),
                new InjectionDetector(90.0),
                new RateAbuseDetector(60, 60, 15, 3, 65.0),
                new EnumerationDetector(5, 60, 80.0),
                new SensitiveDataExposureDetector(85.0)
        );
        engine = new ThreatDetectionEngine(detectors);
    }

    @Test
    @DisplayName("Engine executes all detectors independently and returns multiple findings when present")
    void testEngineEvaluatesMultipleThreats() {
        // Request containing both SQLi in query AND SSN in body
        String body = "{\"applicant\":\"John\",\"ssn\":\"123-45-6789\"}";
        ApiRequestContext context = new ApiRequestContext(
                "req-multi", "POST", "/api/v1/apply", "id=1' OR 1=1", "198.51.100.1", Instant.now(),
                Map.of(), Map.of("id", "1' OR 1=1"), body, body.length(),
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        List<DetectionResult> findings = engine.evaluate(context);
        assertThat(findings).hasSizeGreaterThanOrEqualTo(2);

        List<ThreatType> types = findings.stream().map(DetectionResult::threatType).toList();
        assertThat(types).contains(ThreatType.SQL_INJECTION, ThreatType.DATA_EXFILTRATION);
    }

    @Test
    @DisplayName("Clean request produces empty findings list")
    void testCleanRequestReturnsNoFindings() {
        ApiRequestContext context = new ApiRequestContext(
                "req-clean", "GET", "/api/v1/health", null, "198.51.100.1", Instant.now(),
                Map.of(), Map.of(), "", 0,
                AuthStatus.ANONYMOUS, "NONE", null, "agent"
        );

        List<DetectionResult> findings = engine.evaluate(context);
        assertThat(findings).isEmpty();
    }
}
