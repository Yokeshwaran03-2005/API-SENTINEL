package com.apisentinel.common.controller;

import com.apisentinel.common.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        log.debug("Health check requested");
        return ResponseEntity.ok(new HealthResponse("UP", "API Sentinel"));
    }
}
