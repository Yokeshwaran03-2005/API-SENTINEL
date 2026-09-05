// Shared TypeScript type declarations for API SENTINEL

export type SeverityLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type PolicyAction = "ALLOW" | "MONITOR" | "WARN" | "RATE_LIMIT" | "BLOCK";

export type RequestVerdict = "ALLOWED" | "MONITORED" | "WARNED" | "RATE_LIMITED" | "BLOCKED";

export interface SecurityEventDto {
  id: number;
  eventId: string;
  threatType: string;
  severity: SeverityLevel;
  threatScore: number;
  reason: string;
  actionTaken: PolicyAction;
  endpoint: string;
  source: string;
  evidence: string;
  timestamp: string;
}

export interface ThreatDetectionDto {
  detected: boolean;
  threatType: string;
  severity: SeverityLevel;
  scoreContribution: number;
  reason: string;
  evidence: string;
}

export interface SecurityEventDetailDto extends SecurityEventDto {
  apiRequestId?: string;
  detections: ThreatDetectionDto[];
}

export interface SecurityStatisticsDto {
  totalRequests: number;
  blockedRequests: number;
  rateLimitedRequests: number;
  allowedRequests: number;
  totalSecurityEvents: number;
  averageThreatScore: number;
  eventsBySeverity: {
    LOW: number;
    MEDIUM: number;
    HIGH: number;
    CRITICAL: number;
  };
  eventsByThreatType: Record<string, number>;
  activeBlockedSources: number;
  recentEvents: SecurityEventDto[];
}

export interface ApiEndpointDto {
  id: number;
  pathPattern: string;
  httpMethod: string;
  description: string;
  sensitivityLevel: SeverityLevel;
  rateLimitPerMinute: number;
  active: boolean;
  createdAt: string;
  requestCount?: number;
  threatCount?: number;
  riskScore?: number;
}

export interface ApiRequestDto {
  id: number;
  requestId: string;
  httpMethod: string;
  path: string;
  sourceIp: string;
  userAgent: string;
  responseStatus: number | null;
  authStatus: string;
  clientIdentifier: string;
  requestSizeBytes: number;
  responseSizeBytes: number;
  latencyMs: number;
  verdict: RequestVerdict;
  threatScore: number;
  timestamp: string;
}

export interface ApiRequestDetailDto extends ApiRequestDto {
  headers?: Record<string, string>;
  queryParams?: Record<string, string>;
  verdictReason?: string;
}

export interface SecurityPolicyDto {
  id: number;
  name: string;
  description: string;
  pathPattern: string;
  threatScoreThreshold: number;
  action: PolicyAction;
  rateLimitPerMinute: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateSecurityPolicyRequest {
  name: string;
  description?: string;
  pathPattern: string;
  threatScoreThreshold: number;
  action: PolicyAction;
  rateLimitPerMinute: number;
  active: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface SimulatorScenario {
  id: string;
  name: string;
  threatType: string;
  description: string;
  defaultEndpoint: string;
  defaultMethod: string;
  samplePayload: string;
  expectedSeverity: SeverityLevel;
}

export interface SimulationResult {
  id: string;
  scenarioId: string;
  scenarioName: string;
  requestIndex: number;
  endpoint: string;
  method: string;
  detected: boolean;
  threatType: string;
  score: number;
  riskLevel: SeverityLevel;
  action: PolicyAction;
  reason: string;
  evidence: string;
  statusCode: number;
  latencyMs: number;
  timestamp: string;
}
