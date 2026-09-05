# API SENTINEL Documentation

Comprehensive architecture, design decisions, and system specifications for **API SENTINEL: Real-Time API Security & Threat Control Platform**.

## Contents
* `architecture.md`: Cloud deployment topology, request flow, and data pipelines.
* `threat_model.md`: OWASP API Security Top 10 coverage, detection algorithms, and scoring criteria.
* `api_specification.md`: REST and WebSocket interface definitions between Next.js and Spring Boot.
* `deployment_guide.md`: Step-by-step instructions for Vercel (Frontend), Render (Backend), and Cloud MySQL.

## Architecture Highlights
- **Edge Layer**: Frontend running on Vercel Edge / Serverless for minimal client latency.
- **Security Core**: Low-latency Spring Boot service deployed on Render, processing real-time traffic inspections.
- **State & Persistence**: High-availability cloud-hosted MySQL instance for threat intelligence and historical audit trails.
