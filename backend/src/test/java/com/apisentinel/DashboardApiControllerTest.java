package com.apisentinel;

import com.apisentinel.endpoints.ApiEndpoint;
import com.apisentinel.endpoints.ApiEndpointRepository;
import com.apisentinel.endpoints.HttpMethod;
import com.apisentinel.endpoints.SensitivityLevel;
import com.apisentinel.events.*;
import com.apisentinel.gateway.ApiRequest;
import com.apisentinel.gateway.ApiRequestRepository;
import com.apisentinel.gateway.RequestVerdict;
import com.apisentinel.policy.PolicyAction;
import com.apisentinel.policy.PolicyType;
import com.apisentinel.policy.SecurityPolicy;
import com.apisentinel.policy.SecurityPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private ApiRequestRepository apiRequestRepository;

    @Autowired
    private SecurityPolicyRepository securityPolicyRepository;

    private SecurityEvent testEvent;
    private ApiEndpoint testEndpoint;
    private ApiRequest testRequest;
    private SecurityPolicy testPolicy;

    @BeforeEach
    void setUpTestData() {
        // Seed test SecurityEvent
        testEvent = new SecurityEvent(
                "evt-" + UUID.randomUUID(),
                ThreatType.SQL_INJECTION,
                ThreatSeverity.HIGH,
                75.0,
                "SQL injection in user query",
                MitigationAction.BLOCKED,
                "/api/users",
                "198.51.100.1"
        );
        testEvent = securityEventRepository.save(testEvent);

        // Seed test ApiEndpoint
        if (apiEndpointRepository.findByPathAndHttpMethod("/api/v1/payments", HttpMethod.POST).isEmpty()) {
            testEndpoint = new ApiEndpoint("/api/v1/payments", HttpMethod.POST, "Payment Processing", SensitivityLevel.CRITICAL);
            testEndpoint = apiEndpointRepository.save(testEndpoint);
        } else {
            testEndpoint = apiEndpointRepository.findByPathAndHttpMethod("/api/v1/payments", HttpMethod.POST).get();
        }

        // Seed test ApiRequest
        testRequest = new ApiRequest("req-" + UUID.randomUUID(), "POST", "/api/v1/payments", "198.51.100.1");
        testRequest.setVerdict(RequestVerdict.ALLOWED);
        testRequest.setThreatScore(10.0);
        testRequest = apiRequestRepository.save(testRequest);

        // Seed test SecurityPolicy
        if (securityPolicyRepository.findByName("Dashboard Test Policy").isEmpty()) {
            testPolicy = new SecurityPolicy(
                    "Dashboard Test Policy",
                    PolicyType.THREAT_THRESHOLD,
                    PolicyAction.BLOCK,
                    "/api/test/**",
                    100,
                    60
            );
            testPolicy.setThreatScoreThreshold(80.0);
            testPolicy = securityPolicyRepository.save(testPolicy);
        } else {
            testPolicy = securityPolicyRepository.findByName("Dashboard Test Policy").get();
        }
    }

    @Nested
    @DisplayName("1. Security Events API (/api/security/events)")
    class SecurityEventsApiTests {

        @Test
        @DisplayName("GET /api/security/events returns paginated event list")
        void testGetEventsPaginated() throws Exception {
            mockMvc.perform(get("/api/security/events?page=0&size=10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", not(empty())))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.content[0].eventId", notNullValue()))
                    .andExpect(jsonPath("$.content[0].threatType", notNullValue()))
                    .andExpect(jsonPath("$.content[0].severity", notNullValue()));
        }

        @Test
        @DisplayName("GET /api/security/events with filtering by severity")
        void testGetEventsFilteredBySeverity() throws Exception {
            mockMvc.perform(get("/api/security/events?severity=HIGH")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", not(empty())))
                    .andExpect(jsonPath("$.content[0].severity").value("HIGH"));
        }

        @Test
        @DisplayName("GET /api/security/events/{id} returns single event details")
        void testGetEventById() throws Exception {
            mockMvc.perform(get("/api/security/events/" + testEvent.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testEvent.getId()))
                    .andExpect(jsonPath("$.eventId").value(testEvent.getEventId()))
                    .andExpect(jsonPath("$.threatType").value(testEvent.getThreatType().name()))
                    .andExpect(jsonPath("$.severity").value(testEvent.getSeverity().name()))
                    .andExpect(jsonPath("$.actionTaken").value(testEvent.getActionTaken().name()));
        }

        @Test
        @DisplayName("GET /api/security/events/{id} returns 404 for non-existing event")
        void testGetEventByIdNotFound() throws Exception {
            mockMvc.perform(get("/api/security/events/99999999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    @Nested
    @DisplayName("2. Security Statistics API (/api/security/statistics)")
    class SecurityStatisticsApiTests {

        @Test
        @DisplayName("GET /api/security/statistics returns aggregated metrics")
        void testGetSecurityStatistics() throws Exception {
            mockMvc.perform(get("/api/security/statistics")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRequests", greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.totalSecurityEvents", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.averageThreatScore", notNullValue()))
                    .andExpect(jsonPath("$.eventsBySeverity", notNullValue()))
                    .andExpect(jsonPath("$.eventsByThreatType", notNullValue()))
                    .andExpect(jsonPath("$.recentEvents", not(empty())));
        }
    }

    @Nested
    @DisplayName("3. Monitored Endpoints API (/api/endpoints)")
    class MonitoredEndpointsApiTests {

        @Test
        @DisplayName("GET /api/endpoints returns list of endpoints")
        void testGetEndpoints() throws Exception {
            mockMvc.perform(get("/api/endpoints")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(empty())))
                    .andExpect(jsonPath("$[?(@.path == '/api/v1/payments')]", not(empty())));
        }

        @Test
        @DisplayName("GET /api/endpoints with sensitivity filter")
        void testGetEndpointsFiltered() throws Exception {
            mockMvc.perform(get("/api/endpoints?sensitivity=CRITICAL")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(empty())))
                    .andExpect(jsonPath("$[0].sensitivityLevel").value("CRITICAL"));
        }

        @Test
        @DisplayName("GET /api/endpoints/{id} returns single endpoint")
        void testGetEndpointById() throws Exception {
            mockMvc.perform(get("/api/endpoints/" + testEndpoint.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testEndpoint.getId()))
                    .andExpect(jsonPath("$.path").value("/api/v1/payments"))
                    .andExpect(jsonPath("$.name").value("Payment Processing"));
        }

        @Test
        @DisplayName("GET /api/endpoints/{id} returns 404 for non-existent endpoint")
        void testGetEndpointByIdNotFound() throws Exception {
            mockMvc.perform(get("/api/endpoints/9999999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("4. Intercepted Requests API (/api/requests)")
    class InterceptedRequestsApiTests {

        @Test
        @DisplayName("GET /api/requests returns paginated list of requests")
        void testGetRequestsPaginated() throws Exception {
            mockMvc.perform(get("/api/requests?page=0&size=10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", not(empty())))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.content[0].requestId", notNullValue()));
        }

        @Test
        @DisplayName("GET /api/requests/{id} returns request detail")
        void testGetRequestById() throws Exception {
            mockMvc.perform(get("/api/requests/" + testRequest.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testRequest.getId()))
                    .andExpect(jsonPath("$.requestId").value(testRequest.getRequestId()))
                    .andExpect(jsonPath("$.path").value("/api/v1/payments"))
                    .andExpect(jsonPath("$.verdict").value("ALLOWED"));
        }

        @Test
        @DisplayName("GET /api/requests/{id} returns 404 when request not found")
        void testGetRequestByIdNotFound() throws Exception {
            mockMvc.perform(get("/api/requests/9999999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("5. Security Policies API (/api/policies)")
    class SecurityPoliciesApiTests {

        @Test
        @DisplayName("GET /api/policies returns list of policies")
        void testGetPolicies() throws Exception {
            mockMvc.perform(get("/api/policies")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", not(empty())))
                    .andExpect(jsonPath("$[?(@.name == 'Dashboard Test Policy')]", not(empty())));
        }

        @Test
        @DisplayName("GET /api/policies/{id} returns single policy")
        void testGetPolicyById() throws Exception {
            mockMvc.perform(get("/api/policies/" + testPolicy.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testPolicy.getId()))
                    .andExpect(jsonPath("$.name").value("Dashboard Test Policy"))
                    .andExpect(jsonPath("$.threatScoreThreshold").value(80.0));
        }

        @Test
        @DisplayName("PUT /api/policies/{id} updates policy successfully")
        void testUpdatePolicySuccess() throws Exception {
            String updatePayload = """
                    {
                        "name": "Updated Dashboard Policy",
                        "threatScoreThreshold": 75.0,
                        "requestThreshold": 150,
                        "timeWindowSeconds": 30,
                        "actionOnBreach": "BLOCK",
                        "isEnabled": true
                    }
                    """;

            mockMvc.perform(put("/api/policies/" + testPolicy.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testPolicy.getId()))
                    .andExpect(jsonPath("$.name").value("Updated Dashboard Policy"))
                    .andExpect(jsonPath("$.threatScoreThreshold").value(75.0))
                    .andExpect(jsonPath("$.requestThreshold").value(150))
                    .andExpect(jsonPath("$.timeWindowSeconds").value(30));
        }

        @Test
        @DisplayName("PUT /api/policies/{id} with invalid threshold returns 400 Bad Request")
        void testUpdatePolicyValidationFailure() throws Exception {
            String invalidPayload = """
                    {
                        "threatScoreThreshold": 150.0,
                        "requestThreshold": -5
                    }
                    """;

            mockMvc.perform(put("/api/policies/" + testPolicy.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.validationErrors", notNullValue()));
        }

        @Test
        @DisplayName("PUT /api/policies/{id} returns 404 for non-existent policy")
        void testUpdatePolicyNotFound() throws Exception {
            String updatePayload = "{\"name\":\"NonExistent\"}";

            mockMvc.perform(put("/api/policies/9999999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
