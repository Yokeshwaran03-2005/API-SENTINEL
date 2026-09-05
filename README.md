# API SENTINEL
> **Real-Time API Security & Threat Control Platform**

API SENTINEL is an enterprise-grade real-time API security, threat detection, and mitigation platform. It monitors inbound and outbound API traffic, detects anomalies and malicious payloads (SQL injection, XSS, rate-limit abuses, credential stuffing, etc.), scores threat levels dynamically, and enforces security policies at the gateway level.

---

## 🏗️ Architecture Overview

The platform uses a decoupled, high-performance distributed architecture designed for cloud deployment:

* **Frontend**: Next.js (App Router, TypeScript) — Deployed on **Vercel**
* **Backend**: Java Spring Boot 3.x (Maven) — Deployed on **Render**
* **Database**: MySQL 8.x — Hosted on Managed Cloud Database (e.g., PlanetScale, AWS RDS, or Aiven)

```mermaid
graph LR
    User[Client / Browser] -->|HTTPS / WSS| Frontend[Next.js Frontend (Vercel)]
    Frontend -->|REST API / WebSocket| Backend[Spring Boot Backend (Render)]
    Backend -->|JDBC Connection Pool| Database[(MySQL Cloud DB)]
    Backend -->|Telemetry / Events| Gateway[API Security Gateway Engine]
```

---

## 📁 Repository Structure

```
API-SENTINEL/
├── backend/                  # Java Spring Boot backend service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/apisentinel/
│   │   │   │   ├── auth/         # Authentication & Authorization
│   │   │   │   ├── gateway/      # API Gateway & Reverse Proxy Filter
│   │   │   │   ├── detection/    # Threat Detection Rules & Heuristics
│   │   │   │   ├── scoring/      # Dynamic Threat Scoring Engine
│   │   │   │   ├── policy/       # Rate Limiting & Blocking Policies
│   │   │   │   ├── events/       # Real-time Security Event Pipeline
│   │   │   │   ├── endpoints/    # Monitored Endpoints Registry
│   │   │   │   ├── simulator/    # Attack Traffic Simulator
│   │   │   │   ├── config/       # Spring & Security Configurations
│   │   │   │   └── common/       # Shared Utilities, DTOs & Constants
│   │   │   └── resources/        # Application properties & configs
│   │   └── test/                 # Unit & integration tests
│   ├── pom.xml                   # Maven project descriptor
│   └── README.md
├── frontend/                 # Next.js TypeScript application
│   ├── app/                  # Next.js App Router
│   │   ├── dashboard/        # Main Security Operations Center (SOC)
│   │   ├── threats/          # Live Threat Feed & Incident Details
│   │   ├── endpoints/        # Monitored Endpoints Management
│   │   ├── requests/         # API Request Inspector & Audit Logs
│   │   ├── policies/         # Security Rule & Policy Configuration
│   │   └── simulator/        # Attack Simulation Console
│   ├── components/           # Reusable UI Components
│   ├── lib/                  # Utilities, API clients, and helpers
│   ├── public/               # Static assets & icons
│   ├── types/                # Shared TypeScript type definitions
│   └── README.md
├── database/                 # Schema definitions, seed data & migrations
│   └── README.md
├── docs/                     # Architecture, specifications & documentation
│   └── README.md
├── docker-compose.yml        # Local development environment container orchestration
└── README.md                 # Project root documentation
```

---

## 🚀 Quick Start (Local Setup)

### Prerequisites
* Java JDK 17 or 21
* Maven 3.8+
* Node.js 18+ & npm
* Docker & Docker Compose

### 1. Database (Docker)
Start the local MySQL database instance:
```bash
docker-compose up -d
```

### 2. Backend (Spring Boot)
```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```

### 3. Frontend (Next.js)
```bash
cd frontend
npm install
npm run dev
```

---

## 🔒 Security & Compliance
API-SENTINEL is built with a zero-trust architecture principle in mind, ensuring all API traffic passes through detection, scoring, and policy validation before routing.
