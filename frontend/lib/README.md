# Shared Libraries & Utilities (`frontend/lib`)

This directory contains client-side helper functions, API wrappers, and connection utilities.

## Structure
* `api.ts`: Typed fetch/Axios client configured for Spring Boot backend endpoints.
* `websocket.ts`: WebSocket client for receiving real-time threat detection alerts and traffic telemetry.
* `utils.ts`: Class name merging, date formatting, threat score calculation helpers, and converters.
* `constants.ts`: Global application constants, status codes, and severity level mappings.
