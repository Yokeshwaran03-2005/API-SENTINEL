package com.apisentinel;

import com.apisentinel.policy.PolicyEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrafficInterceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyEngine policyEngine;

    @Test
    @DisplayName("1. GET request creates ApiRequestContext and executes cleanly")
    void testGetRequestInterception() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("User-Agent", "SentinelTestClient/1.0")
                        .header("X-Forwarded-For", "198.51.100.25, 10.0.0.1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("API Sentinel"));
    }

    @Test
    @DisplayName("2. Sensitive headers like Authorization, X-API-Key, and Cookies are safely masked in ApiRequestContext")
    void testSensitiveHeaderMaskingInContext() throws Exception {
        mockMvc.perform(get("/api/demo/context")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.supersecretjwt")
                        .header("X-API-Key", "live_secret_key_9876543210")
                        .header("Cookie", "SESSIONID=secret-session-id-12345")
                        .header("X-Forwarded-For", "203.0.113.195, 10.0.0.2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpMethod").value("GET"))
                .andExpect(jsonPath("$.path").value("/api/demo/context"))
                .andExpect(jsonPath("$.sourceIp").value("203.0.113.195"))
                .andExpect(jsonPath("$.authStatus").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.authScheme").value("BEARER"))
                .andExpect(jsonPath("$.headers.Authorization", containsString("[MASKED]")))
                .andExpect(jsonPath("$.headers.Authorization", not(containsString("supersecretjwt"))))
                .andExpect(jsonPath("$.headers.X-API-Key", containsString("[MASKED]")))
                .andExpect(jsonPath("$.headers.Cookie").value("[MASKED]"));
    }

    @Test
    @DisplayName("3. POST body is safely read by both interception layer and downstream Spring MVC controller")
    void testBodyCachingAndDownstreamControllerConsumption() throws Exception {
        String jsonPayload = "{\"action\":\"login\",\"username\":\"attacker\",\"payload\":\"' OR 1=1 --\"}";

        mockMvc.perform(post("/api/demo/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
                        .header("X-Real-IP", "203.0.113.88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedBody.action").value("login"))
                .andExpect(jsonPath("$.receivedBody.username").value("attacker"))
                .andExpect(jsonPath("$.receivedBody.payload").value("' OR 1=1 --"))
                .andExpect(jsonPath("$.interceptedPath").value("/api/demo/echo"))
                .andExpect(jsonPath("$.interceptedMethod").value("POST"))
                .andExpect(jsonPath("$.interceptedSourceIp").value("203.0.113.88"));
    }

    @Test
    @DisplayName("4. Blocked source returns HTTP 403 Forbidden with security block payload")
    void testBlockedSourceReturnsHttp403() throws Exception {
        String attackerIp = "198.51.100.99";
        policyEngine.blockSource(attackerIp, "Active threat identified", 10);

        try {
            mockMvc.perform(get("/api/health")
                            .header("X-Real-IP", attackerIp)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.code").value("SECURITY_BLOCK"))
                    .andExpect(jsonPath("$.action").value("BLOCK"))
                    .andExpect(jsonPath("$.reason", containsString("actively blocked by security policy")));
        } finally {
            policyEngine.unblockSource(attackerIp);
        }
    }
}
