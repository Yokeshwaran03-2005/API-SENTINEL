# 🛡️ API SENTINEL
> **Real-Time Autonomous API Security, Threat Detection & Policy Enforcement Platform**

[![Frontend](https://img.shields.io/badge/Frontend-Vercel%20Live-black?style=for-the-badge&logo=vercel)](https://apisentinel-psi.vercel.app)
[![Backend](https://img.shields.io/badge/Backend-Render%20Live-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://api-backend-wc8m.onrender.com/api/health)
[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black?style=for-the-badge&logo=next.js)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue?style=for-the-badge&logo=typescript)](https://www.typescriptlang.org/)

API SENTINEL is an enterprise-grade real-time API security, threat detection, and mitigation platform. It monitors inbound and outbound API traffic, detects anomalies and malicious payloads (SQL injection, XSS, rate-limit abuses, credential stuffing, enumeration, sensitive data exposure), scores threat levels dynamically, and enforces security policies at the gateway level.

---

## 🌐 Live Deployments & Hackathon Demo

* **Live Web Dashboard (Frontend)**: [https://apisentinel-psi.vercel.app](https://apisentinel-psi.vercel.app)
* **Live API Gateway (Backend)**: [https://api-backend-wc8m.onrender.com](https://api-backend-wc8m.onrender.com)
* **API Health Check**: [https://api-backend-wc8m.onrender.com/api/health](https://api-backend-wc8m.onrender.com/api/health)

---

## 🏗️ Architecture Overview

The platform uses a decoupled, high-performance distributed architecture designed for cloud deployment:

* **Frontend**: Next.js 14 (App Router, Tailwind CSS, Lucide, TypeScript) — Deployed on **Vercel**
* **Backend**: Java Spring Boot 3.x (Spring Security, Spring Data JPA, Actuator) — Containerized & Deployed on **Render**
* **Database**: PostgreSQL (Cloud Hosted on Render) / MySQL 8.x (Docker Local)
* **Monorepo**: Single unified repository housing both `frontend/` and `backend/` services.

```mermaid
graph LR
    User[Client / Browser] -->|HTTPS / WSS| Frontend[Next.js Frontend (Vercel)]
    Frontend -->|REST API / Security Telemetry| Backend[Spring Boot Backend (Render)]
    Backend -->|JDBC Connection Pool| Database[(PostgreSQL / MySQL Cloud DB)]
    Backend -->|Traffic Interception| Gateway[API Security Gateway Engine]
```

---

## 📁 Repository Structure

```
API-SENTINEL/
├── backend/                  # Java Spring Boot backend service
│   ├── Dockerfile            # Multi-stage production container build
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/apisentinel/
│   │   │   │   ├── auth/         # Authentication & Authorization
│   │   │   │   ├── gateway/      # API Gateway & Reverse Proxy Filter
│   │   │   │   ├── detection/    # Threat Detection Rules & Heuristics (OWASP)
│   │   │   │   ├── scoring/      # Dynamic Threat Scoring Engine (0-100)
│   │   │   │   ├── policy/       # Rate Limiting & Blocking Policies
│   │   │   │   ├── events/       # Real-time Security Event Pipeline
│   │   │   │   ├── endpoints/    # Monitored Endpoints Registry
│   │   │   │   ├── simulator/    # Attack Traffic Simulator
│   │   │   │   ├── config/       # Spring & Security Configurations (CORS)
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
│   │   └── simulator/        # Interactive Attack Simulation Console
│   ├── components/           # Reusable UI Components
│   ├── lib/                  # API client & data fetchers
│   ├── types/                # Shared TypeScript type definitions
│   └── README.md
├── database/                 # Schema definitions, seed data & migrations
│   ├── schema.sql
│   └── README.md
├── docs/                     # Architecture, specifications & documentation
│   └── README.md
├── docker-compose.yml        # Local development environment container orchestration
└── README.md                 # Project root documentation
```

---

## ☁️ Cloud Deployment Guide (From this Monorepo)

Both **Vercel** and **Render** natively support deploying from a single monorepo repository using **Root Directory** settings:

### 1. Frontend on Vercel
1. In Vercel, click **Add New Project** and select this repository (`API-SENTINEL`).
2. Under **Root Directory**, click **Edit** and choose `frontend`.
3. Set **Framework Preset** to `Next.js`.
4. In **Environment Variables**, add:
   * `NEXT_PUBLIC_API_URL`: `https://api-backend-wc8m.onrender.com`
5. Click **Deploy**.

### 2. Backend on Render
1. In Render Dashboard, click **New +** -> **Web Service**.
2. Connect this repository (`API-SENTINEL`).
3. Set the following settings:
   * **Root Directory**: `backend`
   * **Runtime**: `Docker` (Render automatically detects `backend/Dockerfile`)
   * **Port**: `8080`
4. In **Environment Variables**, add:
   * `PORT`: `8080`
   * `CORS_ALLOWED_ORIGINS`: `https://apisentinel-psi.vercel.app,http://localhost:3000`
   * `DB_URL`: `jdbc:postgresql://<host>:5432/<database>`
   * `DB_USERNAME`: `<username>`
   * `DB_PASSWORD`: `<password>`
5. Click **Create Web Service**.

---

## 🚀 Quick Start (Local Setup)

### Prerequisites
* Java JDK 17 or 21
* Maven 3.8+
* Node.js 18+ & npm
* Docker & Docker Compose (optional for local database)

### 1. Database (Docker)
Start the local MySQL database instance:
```bash
docker compose up -d
```

### 2. Backend (Spring Boot)
```bash
cd backend
./mvnw clean compile
./mvnw spring-boot:run
```
Backend runs at `http://localhost:8080`.

### 3. Frontend (Next.js)
```bash
cd frontend
npm install
npm run dev
```
Frontend runs at `http://localhost:3000`.

---

## 🔒 Security & Threat Coverage
API-SENTINEL provides comprehensive protection against the OWASP API Security Top 10:
* **SQL Injection & XSS Payloads**: In-flight inspection of body, parameters, and headers.
* **Credential Abuse & Brute Force**: Threshold-based rate limiting and lockouts.
* **Object Enumeration (BOLA/IDOR)**: Pattern detection on sequential resource scraping.
* **Excessive Data Exposure**: Sensitive data tokenization and payload inspection.
* **Autonomous Policy Enforcement**: Instant IP blocking and dynamic threat scoring.
# API-SENTINEL
