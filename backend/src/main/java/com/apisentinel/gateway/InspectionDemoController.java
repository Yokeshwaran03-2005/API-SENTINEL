package com.apisentinel.gateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demonstration and diagnostic controller showcasing that the API request interception layer
 * is actively capturing, wrapping, and constructing the ApiRequestContext.
 */
@RestController
@RequestMapping("/api/demo")
public class InspectionDemoController {

    @GetMapping("/context")
    public ResponseEntity<ApiRequestContext> getCurrentContext() {
        return ApiRequestContextHolder.getContext()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echoWithContext(@RequestBody(required = false) Map<String, Object> body) {
        ApiRequestContext context = ApiRequestContextHolder.getContext().orElse(null);

        return ResponseEntity.ok(Map.of(
                "receivedBody", body != null ? body : Map.of(),
                "interceptedPath", context != null ? context.getPath() : "none",
                "interceptedMethod", context != null ? context.getHttpMethod() : "none",
                "interceptedSourceIp", context != null ? context.getSourceIp() : "none",
                "interceptedAuthStatus", context != null ? context.getAuthStatus().name() : "none",
                "interceptedSize", context != null ? context.getRequestSizeBytes() : 0
        ));
    }
}
