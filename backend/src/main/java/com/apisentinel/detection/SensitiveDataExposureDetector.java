package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic detector for sensitive data exposure, credential exfiltration,
 * and unauthorized metadata harvesting.
 * Inspects sensitive field queries, PII patterns, and internal diagnostic endpoint probes.
 */
@Component
public class SensitiveDataExposureDetector implements ThreatDetector {

    // Sensitive field names that should not be queried or harvested
    private static final Pattern SENSITIVE_FIELD_QUERY_PATTERN = Pattern.compile(
            "(password_hash|passwd|ssn|social_security|credit_card|cvv|private_key|secret_key|api_secret|jwt_secret|client_secret)",
            Pattern.CASE_INSENSITIVE
    );

    // Private key blocks (PEM format)
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
            "-----BEGIN (RSA|EC|OPENSSH|DSA|PGP)?\\s*PRIVATE KEY",
            Pattern.CASE_INSENSITIVE
    );

    // Credit card number pattern (Visa, MasterCard, Amex)
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b"
    );

    // US Social Security Number pattern
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );

    // Dangerous diagnostic and backup routes
    private static final List<String> RECON_ENDPOINTS = List.of(
            "/actuator/env",
            "/actuator/heapdump",
            "/actuator/threaddump",
            "/actuator/configprops",
            "/.env",
            "/.git",
            "/backup.sql",
            "/dump.sql",
            "/id_rsa",
            "/.aws/credentials"
    );

    private final double scoreContribution;

    public SensitiveDataExposureDetector(
            @Value("${sentinel.detection.sensitive.score:85.0}") double scoreContribution
    ) {
        this.scoreContribution = scoreContribution;
    }

    @Override
    public String getDetectorName() {
        return "SensitiveDataExposureDetector";
    }

    @Override
    public DetectionResult detect(ApiRequestContext context) {
        String path = context.getPath();

        // 1. Check for reconnaissance probes targeting sensitive diagnostic endpoints
        if (path != null) {
            String lowerPath = path.toLowerCase();
            for (String reconEndpoint : RECON_ENDPOINTS) {
                if (lowerPath.contains(reconEndpoint)) {
                    return DetectionResult.detected(
                            ThreatType.DATA_EXFILTRATION,
                            ThreatSeverity.HIGH,
                            scoreContribution,
                            "Unauthorized probe against internal/diagnostic resource: " + reconEndpoint,
                            "Targeted endpoint: " + path
                    );
                }
            }
        }

        // 2. Check for sensitive field harvesting in query string or query parameters
        String queryString = context.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            Matcher matcher = SENSITIVE_FIELD_QUERY_PATTERN.matcher(queryString);
            if (matcher.find()) {
                return DetectionResult.detected(
                        ThreatType.DATA_EXFILTRATION,
                        ThreatSeverity.HIGH,
                        scoreContribution,
                        "Query parameter attempts to harvest sensitive field: " + matcher.group(),
                        "Matched query parameter keyword in: " + queryString
                );
            }
        }

        for (Map.Entry<String, String> param : context.getQueryParameters().entrySet()) {
            Matcher keyMatcher = SENSITIVE_FIELD_QUERY_PATTERN.matcher(param.getKey());
            if (keyMatcher.find()) {
                return DetectionResult.detected(
                        ThreatType.DATA_EXFILTRATION,
                        ThreatSeverity.HIGH,
                        scoreContribution,
                        "Attempted retrieval of sensitive field: " + keyMatcher.group(),
                        "Query param: " + param.getKey()
                );
            }
        }

        // 3. Check for high-risk exfiltration artifacts in request body (PII, Private Keys)
        if (context.hasBody()) {
            String body = context.getBody();

            // Check for PEM private keys
            if (PRIVATE_KEY_PATTERN.matcher(body).find()) {
                return DetectionResult.detected(
                        ThreatType.DATA_EXFILTRATION,
                        ThreatSeverity.CRITICAL,
                        scoreContribution + 10.0,
                        "Unencrypted private key detected in request body",
                        "Matched PEM private key header block"
                );
            }

            // Check for Credit Card data leakage
            Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(body);
            if (ccMatcher.find()) {
                return DetectionResult.detected(
                        ThreatType.DATA_EXFILTRATION,
                        ThreatSeverity.HIGH,
                        scoreContribution,
                        "Potential credit card data exposure identified in payload",
                        "Matched payment card numeric pattern in body"
                );
            }

            // Check for SSN leakage
            Matcher ssnMatcher = SSN_PATTERN.matcher(body);
            if (ssnMatcher.find()) {
                return DetectionResult.detected(
                        ThreatType.DATA_EXFILTRATION,
                        ThreatSeverity.HIGH,
                        scoreContribution,
                        "Social Security Number pattern detected in payload",
                        "Matched SSN format in body"
                );
            }
        }

        return DetectionResult.clean();
    }
}
