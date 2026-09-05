package com.apisentinel;

import com.apisentinel.auth.User;
import com.apisentinel.auth.UserRepository;
import com.apisentinel.auth.UserRole;
import com.apisentinel.detection.ThreatDetection;
import com.apisentinel.detection.ThreatDetectionRepository;
import com.apisentinel.endpoints.ApiEndpoint;
import com.apisentinel.endpoints.ApiEndpointRepository;
import com.apisentinel.endpoints.HttpMethod;
import com.apisentinel.endpoints.SensitivityLevel;
import com.apisentinel.events.MitigationAction;
import com.apisentinel.events.SecurityEvent;
import com.apisentinel.events.SecurityEventRepository;
import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequest;
import com.apisentinel.gateway.ApiRequestRepository;
import com.apisentinel.gateway.AuthStatus;
import com.apisentinel.gateway.RequestVerdict;
import com.apisentinel.policy.BlockReason;
import com.apisentinel.policy.BlockedSource;
import com.apisentinel.policy.BlockedSourceRepository;
import com.apisentinel.policy.PolicyAction;
import com.apisentinel.policy.PolicyType;
import com.apisentinel.policy.SecurityPolicy;
import com.apisentinel.policy.SecurityPolicyRepository;
import com.apisentinel.policy.SourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for Phase 3 JPA Entities & Repositories.
 */
@SpringBootTest
@Transactional
class PersistenceLayerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private ApiRequestRepository apiRequestRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private ThreatDetectionRepository threatDetectionRepository;

    @Autowired
    private SecurityPolicyRepository securityPolicyRepository;

    @Autowired
    private BlockedSourceRepository blockedSourceRepository;

    @Test
    @DisplayName("1. User entity persists and queries successfully")
    void testUserPersistence() {
        User user = new User("sec_admin", "admin@apisentinel.io", "hashed_pwd_123", "Security Admin", UserRole.ADMIN);
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);

        Optional<User> found = userRepository.findByUsername("sec_admin");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("admin@apisentinel.io");
    }

    @Test
    @DisplayName("2. ApiEndpoint entity persists with sensitivity rating and method")
    void testApiEndpointPersistence() {
        ApiEndpoint endpoint = new ApiEndpoint("/api/v1/auth/token", HttpMethod.POST, "Token Generation", SensitivityLevel.CRITICAL);
        endpoint.setMaxRequestsPerMinute(30);
        ApiEndpoint saved = apiEndpointRepository.save(endpoint);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSensitivityLevel()).isEqualTo(SensitivityLevel.CRITICAL);

        Optional<ApiEndpoint> found = apiEndpointRepository.findByPathAndHttpMethod("/api/v1/auth/token", HttpMethod.POST);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Token Generation");
    }

    @Test
    @DisplayName("3. ApiRequest persists with telemetry, IP, status, and linked endpoint")
    void testApiRequestPersistence() {
        ApiEndpoint endpoint = apiEndpointRepository.save(
                new ApiEndpoint("/api/v1/data", HttpMethod.GET, "Data API", SensitivityLevel.LOW)
        );

        String reqId = UUID.randomUUID().toString();
        ApiRequest req = new ApiRequest(reqId, "GET", "/api/v1/data", "192.168.1.50");
        req.setUserAgent("Mozilla/5.0 (Windows NT 10.0)");
        req.setResponseStatus(200);
        req.setAuthStatus(AuthStatus.AUTHENTICATED);
        req.setClientIdentifier("client_corp_99");
        req.setRequestSizeBytes(1024L);
        req.setResponseSizeBytes(4096L);
        req.setLatencyMs(45L);
        req.setVerdict(RequestVerdict.ALLOWED);
        req.setThreatScore(5.0);
        req.setEndpoint(endpoint);

        ApiRequest saved = apiRequestRepository.save(req);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTimestamp()).isNotNull();
        assertThat(saved.getEndpoint().getId()).isEqualTo(endpoint.getId());

        Optional<ApiRequest> found = apiRequestRepository.findByRequestId(reqId);
        assertThat(found).isPresent();
        assertThat(found.get().getSourceIp()).isEqualTo("192.168.1.50");
        assertThat(found.get().getVerdict()).isEqualTo(RequestVerdict.ALLOWED);
    }

    @Test
    @DisplayName("4. SecurityEvent & ThreatDetection cascade relationship persists properly")
    void testSecurityEventAndThreatDetectionPersistence() {
        String eventId = UUID.randomUUID().toString();
        SecurityEvent event = new SecurityEvent(
                eventId,
                ThreatType.SQL_INJECTION,
                ThreatSeverity.CRITICAL,
                95.0,
                "Union-based SQL injection detected in query parameter 'id'",
                MitigationAction.BLOCKED,
                "/api/v1/users?id=1' UNION SELECT * --",
                "203.0.113.195"
        );
        event.setEvidence("SELECT * FROM users WHERE id = '1' UNION SELECT null, username, password FROM admin --");

        ThreatDetection detection1 = new ThreatDetection(
                "RULE-SQLI-01",
                "Union Based Pattern",
                "INJECTION",
                0.98,
                "UNION SELECT null, username"
        );
        detection1.setPayloadLocation("QUERY:id");

        ThreatDetection detection2 = new ThreatDetection(
                "RULE-SQLI-04",
                "Comment Terminator Pattern",
                "INJECTION",
                0.92,
                "--"
        );
        detection2.setPayloadLocation("QUERY:id");

        event.addDetection(detection1);
        event.addDetection(detection2);

        SecurityEvent saved = securityEventRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDetections()).hasSize(2);
        assertThat(saved.getThreatType()).isEqualTo(ThreatType.SQL_INJECTION);
        assertThat(saved.getActionTaken()).isEqualTo(MitigationAction.BLOCKED);

        Optional<SecurityEvent> found = securityEventRepository.findByEventId(eventId);
        assertThat(found).isPresent();
        assertThat(found.get().getSeverity()).isEqualTo(ThreatSeverity.CRITICAL);

        List<ThreatDetection> detections = threatDetectionRepository.findBySecurityEventId(saved.getId());
        assertThat(detections).hasSize(2);
        assertThat(detections.get(0).getRuleId()).startsWith("RULE-SQLI");
    }

    @Test
    @DisplayName("5. SecurityPolicy persists with configurable rate limits, thresholds, and patterns")
    void testSecurityPolicyPersistence() {
        SecurityPolicy policy = new SecurityPolicy(
                "Strict Auth Rate Limit",
                PolicyType.RATE_LIMIT,
                PolicyAction.BLOCK,
                "/api/v1/auth/**",
                10,
                60
        );
        policy.setDescription("Restricts authentication attempts to 10 requests per minute");
        policy.setThreatScoreThreshold(75.0);
        policy.setPriority(10);

        SecurityPolicy saved = securityPolicyRepository.save(policy);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRequestThreshold()).isEqualTo(10);
        assertThat(saved.getTimeWindowSeconds()).isEqualTo(60);
        assertThat(saved.isEnabled()).isTrue();

        Optional<SecurityPolicy> found = securityPolicyRepository.findByName("Strict Auth Rate Limit");
        assertThat(found).isPresent();
        assertThat(found.get().getPolicyType()).isEqualTo(PolicyType.RATE_LIMIT);
        assertThat(found.get().getActionOnBreach()).isEqualTo(PolicyAction.BLOCK);
    }

    @Test
    @DisplayName("6. BlockedSource persists with expiration calculation and status")
    void testBlockedSourcePersistence() {
        Instant expiresAt = Instant.now().plus(2, ChronoUnit.HOURS);
        BlockedSource blocked = new BlockedSource(
                "198.51.100.42",
                SourceType.IP_ADDRESS,
                BlockReason.REPEATED_ATTACKS,
                "Excessive SQL injection payload bursts detected",
                expiresAt,
                false,
                "SYSTEM_AUTO_DEFENSE"
        );

        BlockedSource saved = blockedSourceRepository.save(blocked);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isExpired()).isFalse();

        Optional<BlockedSource> found = blockedSourceRepository.findBySourceValueAndIsActiveTrue("198.51.100.42");
        assertThat(found).isPresent();
        assertThat(found.get().getReason()).isEqualTo(BlockReason.REPEATED_ATTACKS);
    }
}
