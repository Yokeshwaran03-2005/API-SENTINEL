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
 * Deterministic detector for SQL Injection, Command Injection, and Path Traversal.
 * Evaluates URI paths, query parameters, and request body payloads against strict
 * signature heuristics without relying on opaque ML models.
 */
@Component
public class InjectionDetector implements ThreatDetector {

    private static final List<InjectionRule> RULES = List.of(
            new InjectionRule(
                    ThreatType.SQL_INJECTION,
                    ThreatSeverity.CRITICAL,
                    Pattern.compile("('|\")\\s*(OR|AND)\\s*('?\\d+'?|\\w+)\\s*=\\s*('?\\d+'?|\\w+)", Pattern.CASE_INSENSITIVE),
                    "Boolean-based SQL injection tautology pattern (' OR 1=1)"
            ),
            new InjectionRule(
                    ThreatType.SQL_INJECTION,
                    ThreatSeverity.CRITICAL,
                    Pattern.compile("UNION\\s+(ALL\\s+)?SELECT", Pattern.CASE_INSENSITIVE),
                    "Union-based SQL injection query pattern"
            ),
            new InjectionRule(
                    ThreatType.SQL_INJECTION,
                    ThreatSeverity.CRITICAL,
                    Pattern.compile("(;|--)\\s*(DROP\\s+TABLE|DELETE\\s+FROM|INSERT\\s+INTO|UPDATE\\s+\\w+\\s+SET)", Pattern.CASE_INSENSITIVE),
                    "Destructive SQL statement injection"
            ),
            new InjectionRule(
                    ThreatType.SQL_INJECTION,
                    ThreatSeverity.HIGH,
                    Pattern.compile("(WAITFOR\\s+DELAY|SLEEP\\s*\\(|BENCHMARK\\s*\\()", Pattern.CASE_INSENSITIVE),
                    "Time-based blind SQL injection probe"
            ),
            new InjectionRule(
                    ThreatType.PATH_TRAVERSAL,
                    ThreatSeverity.HIGH,
                    Pattern.compile("(\\.\\.[\\/\\\\]|%2e%2e%2f|%2e%2e/|\\.\\.%2f)", Pattern.CASE_INSENSITIVE),
                    "Directory and path traversal sequence (../)"
            ),
            new InjectionRule(
                    ThreatType.XSS,
                    ThreatSeverity.HIGH,
                    Pattern.compile("(<script|javascript:|onerror\\s*=|onload\\s*=|eval\\s*\\()", Pattern.CASE_INSENSITIVE),
                    "Cross-Site Scripting (XSS) payload pattern"
            ),
            new InjectionRule(
                    ThreatType.SUSPICIOUS_PAYLOAD,
                    ThreatSeverity.CRITICAL,
                    Pattern.compile("(cat\\s+/etc/passwd|/bin/sh|/bin/bash|cmd\\.exe|powershell\\s+-enc)", Pattern.CASE_INSENSITIVE),
                    "Operating system command injection vector"
            )
    );

    private final double scoreContribution;

    public InjectionDetector(
            @Value("${sentinel.detection.injection.score:90.0}") double scoreContribution
    ) {
        this.scoreContribution = scoreContribution;
    }

    @Override
    public String getDetectorName() {
        return "InjectionDetector";
    }

    @Override
    public DetectionResult detect(ApiRequestContext context) {
        // 1. Inspect URI path and raw query string
        String path = context.getPath();
        DetectionResult pathResult = inspectString(path, "URI Path");
        if (pathResult.detected()) {
            return pathResult;
        }

        String queryString = context.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            DetectionResult queryResult = inspectString(queryString, "Query String");
            if (queryResult.detected()) {
                return queryResult;
            }
        }

        // 2. Inspect query parameters
        for (Map.Entry<String, String> param : context.getQueryParameters().entrySet()) {
            DetectionResult paramResult = inspectString(param.getValue(), "Query Param [" + param.getKey() + "]");
            if (paramResult.detected()) {
                return paramResult;
            }
        }

        // 3. Inspect request body if present
        if (context.hasBody()) {
            DetectionResult bodyResult = inspectString(context.getBody(), "Request Body");
            if (bodyResult.detected()) {
                return bodyResult;
            }
        }

        return DetectionResult.clean();
    }

    private DetectionResult inspectString(String input, String location) {
        if (input == null || input.isBlank()) {
            return DetectionResult.clean();
        }

        for (InjectionRule rule : RULES) {
            Matcher matcher = rule.pattern.matcher(input);
            if (matcher.find()) {
                String matchedSnippet = matcher.group();
                return DetectionResult.detected(
                        rule.threatType,
                        rule.severity,
                        scoreContribution,
                        rule.description + " detected in " + location,
                        "Evidence at " + location + ": " + matchedSnippet
                );
            }
        }

        return DetectionResult.clean();
    }

    private record InjectionRule(
            ThreatType threatType,
            ThreatSeverity severity,
            Pattern pattern,
            String description
    ) {
    }
}
