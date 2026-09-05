# API SENTINEL - Backend Engine

The backend service for **API SENTINEL** is built with Java 17/21 and Spring Boot 3. It serves as the real-time API security gateway, threat analysis engine, scoring calculator, and policy enforcement controller.

## Target Deployment
* **Platform**: [Render](https://render.com) (Web Service / Docker)
* **Runtime**: Java 17/21 (OpenJDK)
* **Build Tool**: Maven

## Package Structure

```
com.apisentinel
├── auth          # Authentication, JWT handling, API keys, RBAC
├── gateway       # Real-time traffic interception, proxy filters, request/response capture
├── detection     # Anomaly detection, signature matching (SQLi, XSS, SSRF, BOLA)
├── scoring       # Real-time risk and threat severity scoring algorithms
├── policy        # Rate limiting, IP blacklisting/whitelisting, automated mitigation rules
├── events        # Real-time security event dispatching & WebSocket publisher
├── endpoints     # Monitored API inventory, status check, and metrics collection
├── simulator     # Attack traffic generation and vulnerability simulation harness
├── config        # Spring security, CORS, WebSocket, and database configurations
└── common        # Shared DTOs, custom exceptions, constants, and utilities
```

## Running Locally

```bash
mvn clean install
mvn spring-boot:run
```
