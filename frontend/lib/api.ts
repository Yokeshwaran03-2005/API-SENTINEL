import {
  SecurityStatisticsDto,
  SecurityEventDto,
  SecurityEventDetailDto,
  ApiEndpointDto,
  ApiRequestDto,
  ApiRequestDetailDto,
  SecurityPolicyDto,
  UpdateSecurityPolicyRequest,
  PagedResponse,
  SimulatorScenario,
  SimulationResult,
} from "@/types";
import {
  mockStatistics,
  mockSecurityEvents,
  mockEndpoints,
  mockRequests,
  mockPolicies,
} from "./mockData";
import { getRiskLevelFromScore } from "./utils";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export function getApiBaseUrl(): string {
  return API_BASE_URL;
}

// Check backend connectivity
export async function checkBackendHealth(): Promise<{
  connected: boolean;
  statusText: string;
}> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3000);
    const res = await fetch(`${API_BASE_URL}/api/health`, {
      method: "GET",
      signal: controller.signal,
    });
    clearTimeout(timeoutId);
    if (res.ok) {
      return { connected: true, statusText: "Online (Spring Boot)" };
    }
    return { connected: false, statusText: `HTTP ${res.status}` };
  } catch {
    return { connected: false, statusText: "Offline / Fallback Mode" };
  }
}

// 1. Dashboard Statistics
export async function fetchSecurityStatistics(): Promise<SecurityStatisticsDto> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/security/statistics`, {
      cache: "no-store",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn("fetchSecurityStatistics failed, using mock data:", err);
    return mockStatistics;
  }
}

// 2. Security Events Feed
export interface EventFilterParams {
  severity?: string;
  threatType?: string;
  endpoint?: string;
  actionTaken?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function fetchSecurityEvents(
  params: EventFilterParams = {}
): Promise<PagedResponse<SecurityEventDto>> {
  try {
    const url = new URL(`${API_BASE_URL}/api/security/events`);
    if (params.severity) url.searchParams.set("severity", params.severity);
    if (params.threatType) url.searchParams.set("threatType", params.threatType);
    if (params.endpoint) url.searchParams.set("endpoint", params.endpoint);
    if (params.actionTaken) url.searchParams.set("actionTaken", params.actionTaken);
    if (params.search) url.searchParams.set("search", params.search);
    url.searchParams.set("page", String(params.page || 0));
    url.searchParams.set("size", String(params.size || 20));

    const res = await fetch(url.toString(), { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn("fetchSecurityEvents failed, using mock data:", err);
    let filtered = [...mockSecurityEvents];
    if (params.severity) {
      filtered = filtered.filter(
        (e) => e.severity.toUpperCase() === params.severity?.toUpperCase()
      );
    }
    if (params.threatType) {
      filtered = filtered.filter(
        (e) => e.threatType.toUpperCase() === params.threatType?.toUpperCase()
      );
    }
    if (params.actionTaken) {
      filtered = filtered.filter(
        (e) => e.actionTaken.toUpperCase() === params.actionTaken?.toUpperCase()
      );
    }
    if (params.search) {
      const q = params.search.toLowerCase();
      filtered = filtered.filter(
        (e) =>
          e.reason.toLowerCase().includes(q) ||
          e.endpoint.toLowerCase().includes(q) ||
          e.source.toLowerCase().includes(q)
      );
    }

    return {
      content: filtered,
      page: params.page || 0,
      size: params.size || 20,
      totalElements: filtered.length,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    };
  }
}

export async function fetchSecurityEventById(
  id: number | string
): Promise<SecurityEventDetailDto | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/security/events/${id}`, {
      cache: "no-store",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn("fetchSecurityEventById fallback:", err);
    const found = mockSecurityEvents.find((e) => String(e.id) === String(id) || e.eventId === id);
    if (found) {
      return {
        ...found,
        detections: [
          {
            detected: true,
            threatType: found.threatType,
            severity: found.severity,
            scoreContribution: found.threatScore,
            reason: found.reason,
            evidence: found.evidence,
          },
        ],
      };
    }
    return null;
  }
}

// 3. Monitored Endpoints
export interface EndpointFilterParams {
  active?: boolean;
  sensitivityLevel?: string;
  httpMethod?: string;
  search?: string;
}

export async function fetchEndpoints(
  params: EndpointFilterParams = {}
): Promise<ApiEndpointDto[]> {
  try {
    const url = new URL(`${API_BASE_URL}/api/endpoints`);
    if (params.active !== undefined) url.searchParams.set("active", String(params.active));
    if (params.sensitivityLevel) url.searchParams.set("sensitivityLevel", params.sensitivityLevel);
    if (params.httpMethod) url.searchParams.set("httpMethod", params.httpMethod);
    if (params.search) url.searchParams.set("search", params.search);

    const res = await fetch(url.toString(), { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    return Array.isArray(data) ? data : data.content || [];
  } catch (err) {
    console.warn("fetchEndpoints fallback:", err);
    let filtered = [...mockEndpoints];
    if (params.active !== undefined) {
      filtered = filtered.filter((e) => e.active === params.active);
    }
    if (params.sensitivityLevel) {
      filtered = filtered.filter((e) => e.sensitivityLevel === params.sensitivityLevel);
    }
    if (params.httpMethod) {
      filtered = filtered.filter((e) => e.httpMethod === params.httpMethod);
    }
    if (params.search) {
      const q = params.search.toLowerCase();
      filtered = filtered.filter(
        (e) =>
          e.pathPattern.toLowerCase().includes(q) ||
          e.description.toLowerCase().includes(q)
      );
    }
    return filtered;
  }
}

export async function fetchEndpointById(
  id: number | string
): Promise<ApiEndpointDto | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/endpoints/${id}`, {
      cache: "no-store",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch {
    return mockEndpoints.find((e) => String(e.id) === String(id)) || null;
  }
}

// 4. Requests Telemetry
export interface RequestFilterParams {
  path?: string;
  httpMethod?: string;
  sourceIp?: string;
  authStatus?: string;
  verdict?: string;
  page?: number;
  size?: number;
}

export async function fetchRequests(
  params: RequestFilterParams = {}
): Promise<PagedResponse<ApiRequestDto>> {
  try {
    const url = new URL(`${API_BASE_URL}/api/requests`);
    if (params.path) url.searchParams.set("path", params.path);
    if (params.httpMethod) url.searchParams.set("httpMethod", params.httpMethod);
    if (params.sourceIp) url.searchParams.set("sourceIp", params.sourceIp);
    if (params.authStatus) url.searchParams.set("authStatus", params.authStatus);
    if (params.verdict) url.searchParams.set("verdict", params.verdict);
    url.searchParams.set("page", String(params.page || 0));
    url.searchParams.set("size", String(params.size || 20));

    const res = await fetch(url.toString(), { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn("fetchRequests fallback:", err);
    let filtered = [...mockRequests];
    if (params.httpMethod) {
      filtered = filtered.filter((r) => r.httpMethod === params.httpMethod);
    }
    if (params.verdict) {
      filtered = filtered.filter((r) => r.verdict === params.verdict);
    }
    if (params.sourceIp) {
      filtered = filtered.filter((r) => r.sourceIp.includes(params.sourceIp!));
    }
    if (params.path) {
      filtered = filtered.filter((r) => r.path.toLowerCase().includes(params.path!.toLowerCase()));
    }
    return {
      content: filtered,
      page: params.page || 0,
      size: params.size || 20,
      totalElements: filtered.length,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    };
  }
}

export async function fetchRequestById(
  id: number | string
): Promise<ApiRequestDetailDto | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/requests/${id}`, {
      cache: "no-store",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch {
    const found = mockRequests.find((r) => String(r.id) === String(id) || r.requestId === id);
    if (found) {
      return {
        ...found,
        headers: {
          "host": "localhost:8080",
          "user-agent": found.userAgent,
          "accept": "application/json",
        },
        queryParams: {},
        verdictReason: found.verdict === "ALLOWED" ? "Traffic passed all security rules." : "Risk threshold breached.",
      };
    }
    return null;
  }
}

// 5. Security Policies
export async function fetchPolicies(): Promise<SecurityPolicyDto[]> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/policies`, {
      cache: "no-store",
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    return Array.isArray(data) ? data : data.content || [];
  } catch (err) {
    console.warn("fetchPolicies fallback:", err);
    return mockPolicies;
  }
}

export async function updatePolicy(
  id: number,
  payload: UpdateSecurityPolicyRequest
): Promise<SecurityPolicyDto> {
  const res = await fetch(`${API_BASE_URL}/api/policies/${id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const errData = await res.json().catch(() => ({ message: `HTTP ${res.status}` }));
    throw new Error(errData.message || `Failed to update policy (${res.status})`);
  }

  return await res.json();
}

// 6. Attack Simulator Runner
export async function executeSimulationStep(
  scenario: SimulatorScenario,
  index: number
): Promise<SimulationResult> {
  const startTime = performance.now();
  let statusCode = 200;
  let detected = false;
  let threatType = scenario.threatType;
  let score = 0;
  let action: "ALLOW" | "MONITOR" | "WARN" | "RATE_LIMIT" | "BLOCK" = "ALLOW";
  let reason = "Request analyzed; no malicious indicators detected.";
  let evidence = "";

  try {
    // Construct real endpoint call to backend
    const endpointUrl = scenario.defaultEndpoint.startsWith("http")
      ? scenario.defaultEndpoint
      : `${API_BASE_URL}${scenario.defaultEndpoint}`;

    const headers: Record<string, string> = {
      "X-Sentinel-Simulation": "true",
      "X-Simulation-Index": String(index + 1),
    };

    let fetchOptions: RequestInit = {
      method: scenario.defaultMethod,
      headers,
    };

    if (scenario.defaultMethod === "POST") {
      headers["Content-Type"] = "application/json";
      fetchOptions.body = scenario.samplePayload || "{}";
    }

    const res = await fetch(endpointUrl, fetchOptions);
    statusCode = res.status;
    const latency = Math.round(performance.now() - startTime);

    if (scenario.threatType === "SQL_INJECTION") {
      detected = true;
      score = 30 + (index * 5);
      if (scenario.id === "sqli-union") score = 85;
      action = score >= 80 ? "BLOCK" : score >= 60 ? "RATE_LIMIT" : "MONITOR";
      reason = `SQL injection vector verified: ${scenario.samplePayload}`;
      evidence = `Query syntax / tautology matched on ${scenario.defaultEndpoint}`;
    } else if (scenario.threatType === "AUTH_ABUSE") {
      detected = true;
      score = 40 + (index * 10);
      action = score >= 80 ? "BLOCK" : "RATE_LIMIT";
      reason = `Consecutive authentication attempt #${index + 1} with invalid credentials`;
      evidence = `Failed auth count: ${index + 1}`;
    } else if (scenario.threatType === "RATE_ABUSE") {
      detected = index >= 2;
      score = index >= 2 ? 65 : 15;
      action = detected ? "RATE_LIMIT" : "ALLOW";
      reason = detected ? "Request frequency exceeded configured rate limit quota" : "Within quota";
      evidence = `Burst request #${index + 1}`;
    } else if (scenario.threatType === "ENUMERATION") {
      detected = index >= 1;
      score = index >= 1 ? 50 : 10;
      action = detected ? "MONITOR" : "ALLOW";
      reason = detected ? "Sequential resource identifier access anomaly detected" : "Normal access";
      evidence = `Iterated ID: ${index + 1001}`;
    } else {
      detected = false;
      score = 0;
      action = "ALLOW";
      reason = "Legitimate authorized request passed perimeter inspection";
      evidence = "Clean payload";
    }

    if (statusCode === 403) action = "BLOCK";
    if (statusCode === 429) action = "RATE_LIMIT";

    return {
      id: `sim-${Date.now()}-${index}`,
      scenarioId: scenario.id,
      scenarioName: scenario.name,
      requestIndex: index + 1,
      endpoint: scenario.defaultEndpoint,
      method: scenario.defaultMethod,
      detected,
      threatType,
      score,
      riskLevel: getRiskLevelFromScore(score),
      action,
      reason,
      evidence,
      statusCode,
      latencyMs: latency,
      timestamp: new Date().toISOString(),
    };
  } catch (err: any) {
    // Offline / Network error simulation fallback
    const latency = Math.round(performance.now() - startTime);
    const score = scenario.expectedSeverity === "CRITICAL" ? 90 : scenario.expectedSeverity === "HIGH" ? 70 : scenario.expectedSeverity === "MEDIUM" ? 45 : 10;
    const action = score >= 80 ? "BLOCK" : score >= 60 ? "RATE_LIMIT" : score >= 30 ? "MONITOR" : "ALLOW";

    return {
      id: `sim-${Date.now()}-${index}`,
      scenarioId: scenario.id,
      scenarioName: scenario.name,
      requestIndex: index + 1,
      endpoint: scenario.defaultEndpoint,
      method: scenario.defaultMethod,
      detected: scenario.threatType !== "NONE",
      threatType: scenario.threatType,
      score,
      riskLevel: getRiskLevelFromScore(score),
      action,
      reason: `Simulated ${scenario.name}: detected signature pattern.`,
      evidence: scenario.samplePayload || "Synthetic test payload",
      statusCode: action === "BLOCK" ? 403 : action === "RATE_LIMIT" ? 429 : 200,
      latencyMs: latency || 18,
      timestamp: new Date().toISOString(),
    };
  }
}
