package com.apisentinel.scoring;

import com.apisentinel.detection.DetectionResult;
import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThreatScoringEngineTest {

    private ThreatScoringProperties properties;
    private ThreatScoringEngine scoringEngine;

    @BeforeEach
    void setUp() {
        properties = new ThreatScoringProperties();
        scoringEngine = new ThreatScoringEngine(properties);
    }

    private DetectionResult createFinding(ThreatType threatType, String reason) {
        return DetectionResult.detected(threatType, ThreatSeverity.HIGH, 0.0, reason, "evidence-payload");
    }

    @Nested
    @DisplayName("1. No Threats Evaluation")
    class NoThreatsTests {

        @Test
        @DisplayName("Empty detections returns score 0, LOW risk, and ALLOW action")
        void testEmptyDetections() {
            ThreatScoreResult result = scoringEngine.score(Collections.emptyList());

            assertEquals(0, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
            assertTrue(result.explanation().contains("Total = 0"));
            assertTrue(result.explanation().contains("Risk = LOW"));
            assertTrue(result.explanation().contains("Recommended action = ALLOW"));
        }

        @Test
        @DisplayName("Null detections list returns score 0 safely")
        void testNullDetections() {
            ThreatScoreResult result = scoringEngine.score(null);

            assertEquals(0, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
        }

        @Test
        @DisplayName("Clean (detected=false) results produce score 0")
        void testCleanDetections() {
            ThreatScoreResult result = scoringEngine.score(List.of(DetectionResult.clean()));

            assertEquals(0, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
        }
    }

    @Nested
    @DisplayName("2. Single Threat Evaluation")
    class SingleThreatTests {

        @Test
        @DisplayName("Single Injection threat yields 30 score, MEDIUM risk, MONITOR action")
        void testSingleInjection() {
            DetectionResult sqli = createFinding(ThreatType.SQL_INJECTION, "SQL Injection detected");
            ThreatScoreResult result = scoringEngine.score(List.of(sqli));

            assertEquals(30, result.totalScore());
            assertEquals(RiskLevel.MEDIUM, result.riskLevel());
            assertEquals(RecommendedAction.MONITOR, result.recommendedAction());
            assertTrue(result.explanation().contains("Injection = 30"));
            assertTrue(result.explanation().contains("Total = 30"));
            assertTrue(result.explanation().contains("Risk = MEDIUM"));
            assertTrue(result.explanation().contains("Recommended action = MONITOR"));
        }

        @Test
        @DisplayName("Single Authentication Abuse threat yields 20 score, LOW risk, ALLOW action")
        void testSingleAuthenticationAbuse() {
            DetectionResult auth = createFinding(ThreatType.BRUTE_FORCE, "Brute force authentication");
            ThreatScoreResult result = scoringEngine.score(List.of(auth));

            assertEquals(20, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
            assertTrue(result.explanation().contains("Authentication Abuse = 20"));
        }

        @Test
        @DisplayName("Single Rate Abuse threat yields 20 score, LOW risk, ALLOW action")
        void testSingleRateAbuse() {
            DetectionResult rate = createFinding(ThreatType.RATE_LIMIT_EXCEEDED, "Volumetric burst limit exceeded");
            ThreatScoreResult result = scoringEngine.score(List.of(rate));

            assertEquals(20, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
            assertTrue(result.explanation().contains("Rate Abuse = 20"));
        }

        @Test
        @DisplayName("Single Enumeration threat yields 15 score, LOW risk, ALLOW action")
        void testSingleEnumeration() {
            DetectionResult enumeration = createFinding(ThreatType.BOLA_IDOR, "BOLA IDOR scan");
            ThreatScoreResult result = scoringEngine.score(List.of(enumeration));

            assertEquals(15, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
            assertTrue(result.explanation().contains("Enumeration = 15"));
        }

        @Test
        @DisplayName("Single Sensitive Data Exposure threat yields 15 score, LOW risk, ALLOW action")
        void testSingleSensitiveDataExposure() {
            DetectionResult sensitive = createFinding(ThreatType.DATA_EXFILTRATION, "Sensitive probe");
            ThreatScoreResult result = scoringEngine.score(List.of(sensitive));

            assertEquals(15, result.totalScore());
            assertEquals(RiskLevel.LOW, result.riskLevel());
            assertEquals(RecommendedAction.ALLOW, result.recommendedAction());
            assertTrue(result.explanation().contains("Sensitive Data Exposure = 15"));
        }
    }

    @Nested
    @DisplayName("3. Multiple Threats & User Specification Example")
    class MultipleThreatsTests {

        @Test
        @DisplayName("User Example: Injection (30) + Rate Abuse (20) + Enumeration (15) = 65, HIGH, RATE_LIMIT")
        void testUserSpecificationExample() {
            List<DetectionResult> findings = List.of(
                    createFinding(ThreatType.SQL_INJECTION, "SQL Injection detected"),
                    createFinding(ThreatType.RATE_LIMIT_EXCEEDED, "Rate limit exceeded"),
                    createFinding(ThreatType.BOLA_IDOR, "BOLA IDOR scan")
            );

            ThreatScoreResult result = scoringEngine.score(findings);

            assertEquals(65, result.totalScore());
            assertEquals(RiskLevel.HIGH, result.riskLevel());
            assertEquals(RecommendedAction.RATE_LIMIT, result.recommendedAction());

            String explanation = result.explanation();
            assertTrue(explanation.contains("Injection = 30"));
            assertTrue(explanation.contains("Rate Abuse = 20"));
            assertTrue(explanation.contains("Enumeration = 15"));
            assertTrue(explanation.contains("Total = 65"));
            assertTrue(explanation.contains("Risk = HIGH"));
            assertTrue(explanation.contains("Recommended action = RATE_LIMIT"));
        }

        @Test
        @DisplayName("High risk without Rate Abuse yields WARN action")
        void testHighRiskWithoutRateAbuseYieldsWarn() {
            // Injection (30) + Authentication Abuse (20) + Sensitive Data (15) = 65
            List<DetectionResult> findings = List.of(
                    createFinding(ThreatType.SQL_INJECTION, "SQL Injection detected"),
                    createFinding(ThreatType.BRUTE_FORCE, "Brute force detected"),
                    createFinding(ThreatType.DATA_EXFILTRATION, "Sensitive data probe")
            );

            ThreatScoreResult result = scoringEngine.score(findings);

            assertEquals(65, result.totalScore());
            assertEquals(RiskLevel.HIGH, result.riskLevel());
            assertEquals(RecommendedAction.WARN, result.recommendedAction());
            assertTrue(result.explanation().contains("Recommended action = WARN"));
        }

        @Test
        @DisplayName("Medium risk combination: Rate Abuse (20) + Enumeration (15) = 35 -> MEDIUM, MONITOR")
        void testMediumRiskCombination() {
            List<DetectionResult> findings = List.of(
                    createFinding(ThreatType.RATE_LIMIT_EXCEEDED, "Burst detected"),
                    createFinding(ThreatType.BOLA_IDOR, "ID scanning detected")
            );

            ThreatScoreResult result = scoringEngine.score(findings);

            assertEquals(35, result.totalScore());
            assertEquals(RiskLevel.MEDIUM, result.riskLevel());
            assertEquals(RecommendedAction.MONITOR, result.recommendedAction());
        }

        @Test
        @DisplayName("Multiple detections within same category are not double-counted")
        void testCategoryDeduplication() {
            // Two injection detections (SQLi and Path Traversal)
            List<DetectionResult> findings = List.of(
                    createFinding(ThreatType.SQL_INJECTION, "SQL Injection"),
                    createFinding(ThreatType.PATH_TRAVERSAL, "Path Traversal")
            );

            ThreatScoreResult result = scoringEngine.score(findings);

            // Both are INJECTION -> category weight is 30, not 60
            assertEquals(30, result.totalScore());
            assertEquals(RiskLevel.MEDIUM, result.riskLevel());
            assertEquals(RecommendedAction.MONITOR, result.recommendedAction());
            assertEquals(2, result.detectionResults().size());
        }
    }

    @Nested
    @DisplayName("4. Maximum Score & Ceiling Clamping")
    class MaximumScoreTests {

        @Test
        @DisplayName("All 5 threats present produces exact maximum score 100, CRITICAL risk, BLOCK action")
        void testAllFiveThreatsMaxScore() {
            List<DetectionResult> allThreats = List.of(
                    createFinding(ThreatType.BRUTE_FORCE, "Auth abuse"),             // 20
                    createFinding(ThreatType.SQL_INJECTION, "SQL injection"),         // 30
                    createFinding(ThreatType.RATE_LIMIT_EXCEEDED, "Rate abuse"),      // 20
                    createFinding(ThreatType.BOLA_IDOR, "Enumeration"),               // 15
                    createFinding(ThreatType.DATA_EXFILTRATION, "Sensitive exposure") // 15
            );

            ThreatScoreResult result = scoringEngine.score(allThreats);

            assertEquals(100, result.totalScore());
            assertEquals(RiskLevel.CRITICAL, result.riskLevel());
            assertEquals(RecommendedAction.BLOCK, result.recommendedAction());

            String exp = result.explanation();
            assertTrue(exp.contains("Authentication Abuse = 20"));
            assertTrue(exp.contains("Injection = 30"));
            assertTrue(exp.contains("Rate Abuse = 20"));
            assertTrue(exp.contains("Enumeration = 15"));
            assertTrue(exp.contains("Sensitive Data Exposure = 15"));
            assertTrue(exp.contains("Total = 100"));
            assertTrue(exp.contains("Risk = CRITICAL"));
            assertTrue(exp.contains("Recommended action = BLOCK"));
        }

        @Test
        @DisplayName("Custom higher weights are clamped to maxScore 100")
        void testCeilingClamping() {
            properties.setWeightForCategory(ThreatCategory.INJECTION, 70);
            properties.setWeightForCategory(ThreatCategory.AUTHENTICATION_ABUSE, 50);
            // 70 + 50 = 120 -> clamped to 100

            List<DetectionResult> findings = List.of(
                    createFinding(ThreatType.SQL_INJECTION, "SQL Injection"),
                    createFinding(ThreatType.BRUTE_FORCE, "Auth abuse")
            );

            ThreatScoreResult result = scoringEngine.score(findings);

            assertEquals(100, result.totalScore());
            assertEquals(RiskLevel.CRITICAL, result.riskLevel());
            assertEquals(RecommendedAction.BLOCK, result.recommendedAction());
        }
    }

    @Nested
    @DisplayName("5. Threshold Boundaries")
    class ThresholdBoundariesTests {

        @Test
        @DisplayName("Boundary at 0: LOW risk, ALLOW")
        void testBoundaryAtZero() {
            assertEquals(RiskLevel.LOW, scoringEngine.evaluateRiskLevel(0));
            assertEquals(RecommendedAction.ALLOW, scoringEngine.resolveAction(RiskLevel.LOW, Collections.emptySet()));
        }

        @Test
        @DisplayName("Boundary at 29: LOW risk, ALLOW")
        void testBoundaryAt29() {
            assertEquals(RiskLevel.LOW, scoringEngine.evaluateRiskLevel(29));
            assertEquals(RecommendedAction.ALLOW, scoringEngine.resolveAction(RiskLevel.LOW, Collections.emptySet()));
        }

        @Test
        @DisplayName("Boundary at 30: MEDIUM risk, MONITOR")
        void testBoundaryAt30() {
            assertEquals(RiskLevel.MEDIUM, scoringEngine.evaluateRiskLevel(30));
            assertEquals(RecommendedAction.MONITOR, scoringEngine.resolveAction(RiskLevel.MEDIUM, Collections.emptySet()));
        }

        @Test
        @DisplayName("Boundary at 59: MEDIUM risk, MONITOR")
        void testBoundaryAt59() {
            assertEquals(RiskLevel.MEDIUM, scoringEngine.evaluateRiskLevel(59));
            assertEquals(RecommendedAction.MONITOR, scoringEngine.resolveAction(RiskLevel.MEDIUM, Collections.emptySet()));
        }

        @Test
        @DisplayName("Boundary at 60: HIGH risk")
        void testBoundaryAt60() {
            assertEquals(RiskLevel.HIGH, scoringEngine.evaluateRiskLevel(60));
            // Without Rate Abuse -> WARN
            assertEquals(RecommendedAction.WARN, scoringEngine.resolveAction(RiskLevel.HIGH, Collections.emptySet()));
            // With Rate Abuse -> RATE_LIMIT
            assertEquals(RecommendedAction.RATE_LIMIT, scoringEngine.resolveAction(RiskLevel.HIGH, java.util.Set.of(ThreatCategory.RATE_ABUSE)));
        }

        @Test
        @DisplayName("Boundary at 79: HIGH risk")
        void testBoundaryAt79() {
            assertEquals(RiskLevel.HIGH, scoringEngine.evaluateRiskLevel(79));
            assertEquals(RecommendedAction.WARN, scoringEngine.resolveAction(RiskLevel.HIGH, Collections.emptySet()));
            assertEquals(RecommendedAction.RATE_LIMIT, scoringEngine.resolveAction(RiskLevel.HIGH, java.util.Set.of(ThreatCategory.RATE_ABUSE)));
        }

        @Test
        @DisplayName("Boundary at 80: CRITICAL risk, BLOCK")
        void testBoundaryAt80() {
            assertEquals(RiskLevel.CRITICAL, scoringEngine.evaluateRiskLevel(80));
            assertEquals(RecommendedAction.BLOCK, scoringEngine.resolveAction(RiskLevel.CRITICAL, Collections.emptySet()));
        }

        @Test
        @DisplayName("Boundary at 100: CRITICAL risk, BLOCK")
        void testBoundaryAt100() {
            assertEquals(RiskLevel.CRITICAL, scoringEngine.evaluateRiskLevel(100));
            assertEquals(RecommendedAction.BLOCK, scoringEngine.resolveAction(RiskLevel.CRITICAL, Collections.emptySet()));
        }
    }

    @Nested
    @DisplayName("6. Scoring Configurability")
    class ConfigurabilityTests {

        @Test
        @DisplayName("Modifying scoring weights dynamically affects score calculation")
        void testDynamicWeightModification() {
            properties.setWeightForCategory(ThreatCategory.INJECTION, 50);

            DetectionResult sqli = createFinding(ThreatType.SQL_INJECTION, "SQL Injection detected");
            ThreatScoreResult result = scoringEngine.score(List.of(sqli));

            assertEquals(50, result.totalScore());
            assertEquals(RiskLevel.MEDIUM, result.riskLevel());
            assertTrue(result.explanation().contains("Injection = 50"));
        }

        @Test
        @DisplayName("Modifying risk thresholds adjusts risk classification")
        void testDynamicThresholdModification() {
            // Lower critical threshold to 50
            properties.setCriticalMin(50);

            assertEquals(RiskLevel.CRITICAL, scoringEngine.evaluateRiskLevel(50));
            assertEquals(RiskLevel.CRITICAL, scoringEngine.evaluateRiskLevel(75));
        }
    }
}
