package com.apisentinel.policy;

import com.apisentinel.detection.DetectionResult;
import com.apisentinel.events.*;
import com.apisentinel.gateway.ApiRequestContext;
import com.apisentinel.gateway.AuthStatus;
import com.apisentinel.scoring.RecommendedAction;
import com.apisentinel.scoring.RiskLevel;
import com.apisentinel.scoring.ThreatScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyEngineTest {

    @Mock
    private BlockedSourceRepository blockedSourceRepository;

    @Mock
    private SecurityPolicyRepository securityPolicyRepository;

    @Mock
    private SecurityEventRepository securityEventRepository;

    private PolicyEngineProperties properties;
    private DefaultPolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        properties = new PolicyEngineProperties();
        policyEngine = new DefaultPolicyEngine(
                properties,
                blockedSourceRepository,
                securityPolicyRepository,
                securityEventRepository
        );
    }

    private ApiRequestContext createDummyContext(String ip, String path) {
        return new ApiRequestContext(
                "req-101",
                "GET",
                path,
                "",
                ip,
                Instant.now(),
                Map.of(),
                Map.of(),
                "",
                0,
                AuthStatus.ANONYMOUS,
                "NONE",
                "anonymous",
                "TestClient"
        );
    }

    private DetectionResult createDetection(ThreatType type, String reason) {
        return DetectionResult.detected(type, ThreatSeverity.HIGH, 0.0, reason, "payload-evidence");
    }

    @Nested
    @DisplayName("1. ALLOW Decision Path")
    class AllowDecisionTests {

        @Test
        @DisplayName("Zero threat score returns ALLOW decision")
        void testZeroScoreReturnsAllow() {
            ApiRequestContext context = createDummyContext("192.168.1.10", "/api/products");
            ThreatScoreResult scoreResult = new ThreatScoreResult(0, RiskLevel.LOW, Collections.emptyList(), RecommendedAction.ALLOW, "clean");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.ALLOW, decision.action());
            assertTrue(decision.isAllowed());
            assertEquals(0, decision.threatScore());
            assertEquals(RiskLevel.LOW, decision.riskLevel());
            verify(blockedSourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Low threat score (20 < 30) returns ALLOW, safe for legitimate users without blocking")
        void testLowScoreReturnsAllow() {
            ApiRequestContext context = createDummyContext("192.168.1.10", "/api/products");
            List<DetectionResult> detections = List.of(createDetection(ThreatType.BRUTE_FORCE, "Minor repeated login delay"));
            ThreatScoreResult scoreResult = new ThreatScoreResult(20, RiskLevel.LOW, detections, RecommendedAction.ALLOW, "low risk");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.ALLOW, decision.action());
            assertTrue(decision.isAllowed());
            assertEquals(20, decision.threatScore());
            assertEquals(RiskLevel.LOW, decision.riskLevel());
            assertFalse(decision.isBlocked());
            // Safe: Never blocks a source on a single weak signal
            assertFalse(policyEngine.isSourceBlocked("192.168.1.10"));
        }
    }

    @Nested
    @DisplayName("2. MONITOR Decision Path")
    class MonitorDecisionTests {

        @Test
        @DisplayName("Medium risk score (30 - 59) returns MONITOR decision and records audit event")
        void testMediumScoreReturnsMonitor() {
            ApiRequestContext context = createDummyContext("192.168.1.20", "/api/search");
            List<DetectionResult> detections = List.of(createDetection(ThreatType.SQL_INJECTION, "SQL syntax anomaly detected"));
            ThreatScoreResult scoreResult = new ThreatScoreResult(30, RiskLevel.MEDIUM, detections, RecommendedAction.MONITOR, "medium risk");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.MONITOR, decision.action());
            assertTrue(decision.isMonitored());
            assertEquals(30, decision.threatScore());
            assertEquals(RiskLevel.MEDIUM, decision.riskLevel());
            assertFalse(decision.isBlocked());

            // SecurityEvent recorded for audit
            verify(securityEventRepository, times(1)).save(any(SecurityEvent.class));
            // Legitimate user safety: source is NOT blocked
            assertFalse(policyEngine.isSourceBlocked("192.168.1.20"));
        }
    }

    @Nested
    @DisplayName("3. WARN Decision Path")
    class WarnDecisionTests {

        @Test
        @DisplayName("High risk score (60 - 79) without rate abuse returns WARN decision")
        void testHighRiskWithoutRateAbuseReturnsWarn() {
            ApiRequestContext context = createDummyContext("192.168.1.30", "/api/users");
            List<DetectionResult> detections = List.of(
                    createDetection(ThreatType.SQL_INJECTION, "Boolean SQL tautology"),
                    createDetection(ThreatType.BRUTE_FORCE, "Repeated failed login"),
                    createDetection(ThreatType.DATA_EXFILTRATION, "Sensitive data harvest")
            );
            ThreatScoreResult scoreResult = new ThreatScoreResult(65, RiskLevel.HIGH, detections, RecommendedAction.WARN, "high risk");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.WARN, decision.action());
            assertTrue(decision.isWarning());
            assertEquals(65, decision.threatScore());
            assertEquals(RiskLevel.HIGH, decision.riskLevel());
            assertFalse(decision.isBlocked());

            verify(securityEventRepository, times(1)).save(any(SecurityEvent.class));
            assertFalse(policyEngine.isSourceBlocked("192.168.1.30"));
        }
    }

    @Nested
    @DisplayName("4. RATE_LIMIT Decision Path")
    class RateLimitDecisionTests {

        @Test
        @DisplayName("High risk score with Rate Abuse threat returns RATE_LIMIT decision")
        void testHighRiskWithRateAbuseReturnsRateLimit() {
            ApiRequestContext context = createDummyContext("192.168.1.40", "/api/orders");
            List<DetectionResult> detections = List.of(
                    createDetection(ThreatType.SQL_INJECTION, "SQL Injection"),
                    createDetection(ThreatType.RATE_LIMIT_EXCEEDED, "Abnormal request frequency"),
                    createDetection(ThreatType.BOLA_IDOR, "High-risk API enumeration")
            );
            ThreatScoreResult scoreResult = new ThreatScoreResult(65, RiskLevel.HIGH, detections, RecommendedAction.RATE_LIMIT, "high risk");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.RATE_LIMIT, decision.action());
            assertTrue(decision.isRateLimited());
            assertEquals(65, decision.threatScore());
            assertEquals(RiskLevel.HIGH, decision.riskLevel());

            verify(securityEventRepository, times(1)).save(any(SecurityEvent.class));
        }

        @Test
        @DisplayName("Volumetric sliding window breach triggers RATE_LIMIT decision")
        void testSlidingWindowRateLimiting() {
            properties.setRateLimitMaxRequests(2);
            properties.setRateLimitWindowSeconds(60);

            ApiRequestContext context = createDummyContext("10.0.0.5", "/api/messages");
            ThreatScoreResult cleanScore = new ThreatScoreResult(0, RiskLevel.LOW, Collections.emptyList(), RecommendedAction.ALLOW, "clean");

            // Req 1 & 2 pass
            assertEquals(PolicyAction.ALLOW, policyEngine.evaluate(context, cleanScore).action());
            assertEquals(PolicyAction.ALLOW, policyEngine.evaluate(context, cleanScore).action());

            // Req 3 exceeds rate limit
            SecurityDecision decision = policyEngine.evaluate(context, cleanScore);
            assertEquals(PolicyAction.RATE_LIMIT, decision.action());
            assertTrue(decision.isRateLimited());
            assertTrue(decision.reason().contains("Abnormal request frequency"));
        }
    }

    @Nested
    @DisplayName("5. BLOCK Decision Path & User Specification Example")
    class BlockDecisionTests {

        @Test
        @DisplayName("User Example: Threat score = 87 yields BLOCK with combined explainable reason")
        void testUserSpecificationExampleScore87() {
            ApiRequestContext context = createDummyContext("203.0.113.99", "/api/v1/accounts");
            List<DetectionResult> detections = List.of(
                    createDetection(ThreatType.BOLA_IDOR, "High-risk API enumeration"),
                    createDetection(ThreatType.RATE_LIMIT_EXCEEDED, "Abnormal request frequency")
            );
            ThreatScoreResult scoreResult = new ThreatScoreResult(87, RiskLevel.CRITICAL, detections, RecommendedAction.BLOCK, "critical");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.BLOCK, decision.action());
            assertTrue(decision.isBlocked());
            assertEquals(87, decision.threatScore());
            assertEquals(RiskLevel.CRITICAL, decision.riskLevel());

            // Matches user's example reason: "High-risk API enumeration combined with abnormal request frequency."
            assertTrue(decision.reason().contains("High-risk API enumeration combined with abnormal request frequency"));

            // Verifies dynamic source block cooldown registered for attacker IP
            assertTrue(policyEngine.isSourceBlocked("203.0.113.99"));

            // Verifies SecurityEvent persisted
            ArgumentCaptor<SecurityEvent> eventCaptor = ArgumentCaptor.forClass(SecurityEvent.class);
            verify(securityEventRepository, atLeastOnce()).save(eventCaptor.capture());
            SecurityEvent captured = eventCaptor.getValue();
            assertEquals(MitigationAction.BLOCKED, captured.getActionTaken());
            assertEquals(87.0, captured.getThreatScore());
            assertEquals(ThreatSeverity.CRITICAL, captured.getSeverity());
        }

        @Test
        @DisplayName("Pre-existing blocked source IP is immediately BLOCKED on access")
        void testPreExistingBlockedSource() {
            String blockedIp = "198.51.100.55";
            BlockedSource blockedSource = new BlockedSource(
                    blockedIp,
                    SourceType.IP_ADDRESS,
                    BlockReason.HIGH_THREAT_SCORE,
                    "Previous attack",
                    Instant.now().plus(10, ChronoUnit.MINUTES),
                    false,
                    "ADMIN"
            );
            when(blockedSourceRepository.findBySourceValueAndIsActiveTrue(blockedIp))
                    .thenReturn(Optional.of(blockedSource));

            ApiRequestContext context = createDummyContext(blockedIp, "/api/profile");
            ThreatScoreResult cleanScore = new ThreatScoreResult(0, RiskLevel.LOW, Collections.emptyList(), RecommendedAction.ALLOW, "clean");

            SecurityDecision decision = policyEngine.evaluate(context, cleanScore);

            assertEquals(PolicyAction.BLOCK, decision.action());
            assertTrue(decision.isBlocked());
            assertTrue(decision.reason().contains("actively blocked by security policy"));
        }

        @Test
        @DisplayName("Endpoint-specific policy threshold breach enforces BLOCK")
        void testEndpointPolicyCheckEnforcesBlock() {
            SecurityPolicy adminPolicy = new SecurityPolicy(
                    "Admin Policy",
                    PolicyType.THREAT_THRESHOLD,
                    PolicyAction.BLOCK,
                    "/api/admin/**",
                    50,
                    60
            );
            adminPolicy.setThreatScoreThreshold(50.0); // Stricter threshold for admin

            when(securityPolicyRepository.findByIsEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(adminPolicy));

            ApiRequestContext context = createDummyContext("192.168.1.75", "/api/admin/system");
            // Score 55 is below default 80 threshold, but breaches admin policy threshold 50
            ThreatScoreResult scoreResult = new ThreatScoreResult(55, RiskLevel.MEDIUM, Collections.emptyList(), RecommendedAction.MONITOR, "medium");

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.BLOCK, decision.action());
            assertTrue(decision.isBlocked());
            assertTrue(decision.reason().contains("Endpoint policy [Admin Policy] breached"));
        }
    }

    @Nested
    @DisplayName("6. Legitimate User Protection & Cooldown Safety")
    class LegitimateUserProtectionTests {

        @Test
        @DisplayName("Dynamic blocking is temporary and unblocks after expiry")
        void testTemporaryBlockCooldown() {
            String ip = "203.0.113.50";
            // Ensure no lingering in-memory active cache
            policyEngine.clearActiveBlockedSources();

            // Simulate expired block in repository
            BlockedSource expiredSource = new BlockedSource(
                    ip,
                    SourceType.IP_ADDRESS,
                    BlockReason.HIGH_THREAT_SCORE,
                    "Expired",
                    Instant.now().minus(5, ChronoUnit.MINUTES),
                    false,
                    "TEST"
            );
            when(blockedSourceRepository.findBySourceValueAndIsActiveTrue(ip))
                    .thenReturn(Optional.of(expiredSource));

            // Policy engine clears expired blocks and returns false
            assertFalse(policyEngine.isSourceBlocked(ip));
            verify(blockedSourceRepository).save(argThat(b -> !b.isActive()));
        }

        @Test
        @DisplayName("Single weak signal does not trigger source block")
        void testWeakSignalNeverBlocks() {
            String legitimateIp = "172.16.0.22";
            ApiRequestContext context = createDummyContext(legitimateIp, "/api/data");

            // Single moderate anomaly (score 45)
            ThreatScoreResult scoreResult = new ThreatScoreResult(
                    45,
                    RiskLevel.MEDIUM,
                    List.of(createDetection(ThreatType.SUSPICIOUS_PAYLOAD, "Unusual user agent")),
                    RecommendedAction.MONITOR,
                    "medium"
            );

            SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

            assertEquals(PolicyAction.MONITOR, decision.action());
            assertFalse(decision.isBlocked());
            assertFalse(policyEngine.isSourceBlocked(legitimateIp));
            verify(blockedSourceRepository, never()).save(any());
        }
    }
}
