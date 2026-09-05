-- =============================================================================
-- API SENTINEL - Conceptual Relational Database Schema (MySQL 8.0+)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS apisentinel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE apisentinel;

-- -----------------------------------------------------------------------------
-- 1. Users Table (RBAC & Platform Operators)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(25) NOT NULL DEFAULT 'VIEWER',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_username (username),
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Monitored API Endpoints Registry
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_endpoints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT 'ANY',
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_monitored BOOLEAN NOT NULL DEFAULT TRUE,
    requires_auth BOOLEAN NOT NULL DEFAULT TRUE,
    max_requests_per_minute INT DEFAULT 60,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_endpoint_path_method UNIQUE (path, http_method),
    INDEX idx_endpoint_path (path),
    INDEX idx_endpoint_sensitivity (sensitivity_level),
    INDEX idx_endpoint_monitored (is_monitored)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. API Requests Log (Telemetry & Security Metadata)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    source_ip VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    response_status INT,
    auth_status VARCHAR(30) NOT NULL DEFAULT 'ANONYMOUS',
    client_identifier VARCHAR(100),
    request_size_bytes BIGINT DEFAULT 0,
    response_size_bytes BIGINT DEFAULT 0,
    latency_ms BIGINT DEFAULT 0,
    verdict VARCHAR(20) NOT NULL DEFAULT 'ALLOWED',
    threat_score DOUBLE DEFAULT 0.0,
    endpoint_id BIGINT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_endpoint FOREIGN KEY (endpoint_id) REFERENCES api_endpoints(id) ON DELETE SET NULL,
    INDEX idx_request_id (request_id),
    INDEX idx_request_source_ip (source_ip),
    INDEX idx_request_path (path),
    INDEX idx_request_timestamp (timestamp),
    INDEX idx_request_verdict (verdict),
    INDEX idx_request_status (response_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Security Events (Threat Incidents)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS security_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    threat_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    threat_score DOUBLE NOT NULL DEFAULT 0.0,
    reason VARCHAR(255) NOT NULL,
    evidence TEXT,
    action_taken VARCHAR(30) NOT NULL DEFAULT 'ALLOWED',
    endpoint VARCHAR(500),
    source VARCHAR(100) NOT NULL,
    api_request_id BIGINT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_request FOREIGN KEY (api_request_id) REFERENCES api_requests(id) ON DELETE SET NULL,
    INDEX idx_sec_event_id (event_id),
    INDEX idx_sec_event_type (threat_type),
    INDEX idx_sec_event_severity (severity),
    INDEX idx_sec_event_score (threat_score),
    INDEX idx_sec_event_source (source),
    INDEX idx_sec_event_time (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Threat Detections (Fine-grained Rule Matches)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS threat_detections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(64) NOT NULL,
    rule_name VARCHAR(120) NOT NULL,
    detection_category VARCHAR(64) NOT NULL,
    confidence_score DOUBLE DEFAULT 1.0,
    matched_pattern VARCHAR(255),
    payload_location VARCHAR(100),
    payload_snippet TEXT,
    security_event_id BIGINT NULL,
    api_request_id BIGINT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_detection_event FOREIGN KEY (security_event_id) REFERENCES security_events(id) ON DELETE CASCADE,
    CONSTRAINT fk_detection_request FOREIGN KEY (api_request_id) REFERENCES api_requests(id) ON DELETE SET NULL,
    INDEX idx_threat_det_rule (rule_id),
    INDEX idx_threat_det_category (detection_category),
    INDEX idx_threat_det_time (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Security Policies (Rules, Rate Limits & Automated Thresholds)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS security_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    policy_type VARCHAR(30) NOT NULL,
    action_on_breach VARCHAR(30) NOT NULL,
    endpoint_pattern VARCHAR(255) NOT NULL DEFAULT '/**',
    request_threshold INT DEFAULT 100,
    time_window_seconds INT DEFAULT 60,
    threat_score_threshold DOUBLE DEFAULT 80.0,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_policy_name (name),
    INDEX idx_policy_type (policy_type),
    INDEX idx_policy_enabled (is_enabled),
    INDEX idx_policy_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. Blocked Sources (IPs, Tokens & Actors Blocklist)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blocked_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_value VARCHAR(120) NOT NULL,
    source_type VARCHAR(25) NOT NULL DEFAULT 'IP_ADDRESS',
    reason VARCHAR(35) NOT NULL DEFAULT 'REPEATED_ATTACKS',
    description VARCHAR(255),
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    is_permanent BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(60) DEFAULT 'SYSTEM',
    INDEX idx_blocked_src_val (source_value),
    INDEX idx_blocked_src_active (is_active),
    INDEX idx_blocked_src_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Seed Initial Security Policies
-- -----------------------------------------------------------------------------
INSERT INTO security_policies (name, description, policy_type, action_on_breach, endpoint_pattern, request_threshold, time_window_seconds, threat_score_threshold, is_enabled, priority)
VALUES 
    ('Global Rate Limit', 'Baseline rate limit protecting the entire API surface', 'RATE_LIMIT', 'RATE_LIMIT', '/**', 120, 60, 85.0, TRUE, 1000),
    ('Auth Shield', 'Strict rate limiting and threat threshold for authentication endpoints', 'RATE_LIMIT', 'BLOCK', '/api/v1/auth/**', 10, 60, 70.0, TRUE, 100),
    ('SQLi Zero Tolerance', 'Immediate block for critical injection payload scores', 'THREAT_THRESHOLD', 'BLOCK', '/**', 0, 0, 75.0, TRUE, 10)
ON DUPLICATE KEY UPDATE name=name;
