# Database Architecture & Schemas

This directory manages the relational database schemas, migrations, and seed data for **API SENTINEL**.

## Database Stack
* **Engine**: MySQL 8.0+
* **Deployment**: Cloud Managed Database (e.g., PlanetScale, AWS RDS MySQL, Aiven)
* **ORM**: Spring Data JPA / Hibernate (Backend)

## Directory Layout
* `schema/`: Initial DDL scripts, table definitions, and constraints.
* `migrations/`: Versioned migration scripts (Flyway / Liquibase compatibility).
* `seeds/`: Test fixtures, synthetic threat data, and default security rule definitions.

## Key Relational Domains (Planned)
1. **Users & Organizations**: Tenant definitions, API keys, and RBAC accounts.
2. **Monitored Endpoints**: Target API routes, health status, and sensitivity ratings.
3. **Threat Signatures & Rules**: Pattern definitions for SQLi, XSS, SSRF, anomaly detection thresholds.
4. **Policy Rules**: Allow/deny lists, IP rate limits, and automated mitigation actions.
5. **Security Events & Audits**: Logged threat incidents, blocked requests, and telemetry history.
