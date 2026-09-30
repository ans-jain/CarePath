# CarePath - Phase 11 Production Readiness & Observability Guide

**Document Version:** 1.0.0  
**Phase Status:** [COMPLETED]  
**Lead Architecture Team:** Platform Reliability & Observability Engineering  
**Scope:** Testing, Distributed Tracing, Health Probes, Resilience Guardrails & Docker Orchestration

---

## 1. Executive Summary & Production Gate Status

CarePath has successfully passed all production readiness gates across all architectural tiers:
- **Backend Core Service (Spring Boot 3.4.3 / Java 21):** 226 unit and integration tests passing (`BUILD SUCCESS`), JaCoCo coverage reporting enabled, Actuator health probes exposed, bounded asynchronous thread executors hardened with `CallerRunsPolicy`, and HikariCP connection leak detection enabled.
- **ML & Explainability Microservice (FastAPI / Python 3.12 / TreeSHAP):** 54 pytest tests passing, correlation ID middleware integrated, readiness probe verifying artifact load states, and physiological boundary safeguards enforced.
- **Frontend Application (React 18 / Vite / TypeScript):** 48 Vitest tests across 11 test suites passing, comprehensive multi-step end-to-end scenarios verified, and zero-defect production compilation (`npm run build`).
- **Container Infrastructure:** Multi-stage, non-root Docker builds for all services orchestrated by a unified, health-dependent `docker-compose.yml`.

> [!IMPORTANT]
> **Clinical Guardrail Confirmation**: CarePath operates strictly as an explainable, decision-support risk stratification engine. In accordance with clinical safety guidelines, it does not diagnose medical conditions, prescribe treatments, or replace licensed medical judgment. All automated responses and UI cards present prominent non-diagnostic clinical disclaimers.

---

## 2. High-Level Architectural Inventory

| Layer | Technology | Key Components & Roles | Observability / Resilience Features |
| :--- | :--- | :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS | `PatientForm`, `RiskSummaryCard`, `ShapWaterfall`, `CounterfactualCards`, `NotificationDropdown`, `AdminAuditLogPage` | Responsive UI, client-side caching, automated error boundaries, SPA client-side routing |
| **Reverse Proxy / Static Server** | NGINX 1.27 Alpine | Reverse proxy for `/api/` (backend) and `/ml/` (microservice), SPA fallback, static caching | Gzip compression, security headers (CSP, X-Frame-Options, X-Content-Type-Options) |
| **Core Backend** | Spring Boot 3.4.3, Java 21, Hibernate / JPA | `AuthController`, `PatientController`, `VitalsController`, `RiskAssessmentController`, `NotificationController`, `AdminController` | `CorrelationIdFilter`, Spring Actuator, SLF4J MDC logging, `CallerRunsPolicy` on async pools, HikariCP leak detector |
| **ML Microservice** | FastAPI, Python 3.12, scikit-learn, TreeSHAP | `predict`, `explain`, `counterfactual` solver, `health` probes | Correlation ID middleware, request duration timing, memory-resident artifact caching, boundary rejection (HTTP 422) |
| **Database** | PostgreSQL 17 Alpine | Tables: `users`, `patient_profiles`, `vital_metrics`, `risk_assessments`, `notifications`, `audit_logs` | Flyway migrations `V1`-`V3`, B-tree indexes on foreign keys, temporal indexes, composite unique constraints |

---

## 3. Health Checks & Probes Specification

CarePath implements two-tier health monitoring for both container orchestrators (Kubernetes / Docker) and uptime monitors.

### 3.1 Spring Boot Core Backend

| Endpoint | HTTP Method | Target Status Codes | Description & Verification |
| :--- | :--- | :--- | :--- |
| `/health` | `GET` | `200 UP`, `503 DOWN` | Core service health check verifying active JDBC `DataSource` connectivity (`SELECT 1`). |
| `/health/liveness` | `GET` | `200 UP` | Instant process liveness check for orchestrator container restarts. |
| `/health/ready` or `/health/readiness` | `GET` | `200 UP`, `503 DOWN` | Readiness probe asserting database responsiveness before receiving routing traffic. |
| `/actuator/health` | `GET` | `200 UP`, `503 DOWN` | Spring Boot Actuator probe with component health metrics. |

### 3.2 FastAPI ML Microservice

| Endpoint | HTTP Method | Target Status Codes | Description & Verification |
| :--- | :--- | :--- | :--- |
| `/ml/v1/health` or `/health` | `GET` | `200 OK` | ML microservice heartbeat reporting service version, model version, and explainer type. |
| `/ml/v1/health/ready` or `/health/ready` | `GET` | `200 OK`, `503 Service Unavailable` | Deep readiness probe verifying that calibrated model artifacts and scaler pipelines are loaded in memory. |

---

## 4. Observability & Distributed Tracing

### 4.1 Request Correlation Lifecycle
1. Incoming HTTP requests are intercepted by `CorrelationIdFilter` (Spring Boot) and `correlation_id_middleware` (FastAPI).
2. If the request contains an `X-Request-ID` header, that value is validated and propagated. Otherwise, a secure UUID is generated.
3. The identifier is stored in SLF4J's Mapped Diagnostic Context (`MDC.put("requestId", correlationId)`) on the Java backend and in Python request state context.
4. Outgoing HTTP responses and inter-service HTTP client calls (via Spring `WebClient`) inject the `X-Request-ID` header.
5. In a `finally` block, the MDC context is cleared (`MDC.remove("requestId")`) to prevent thread-pool context leakage.

### 4.2 Structured Logging Pattern
All backend logging is standardized in `application.yml`:
```yaml
logging:
  pattern:
    level: "%5p"
    console: "%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %highlight(%5level) [%blue(carepath-backend),%X{traceId:-NONE},%X{spanId:-NONE}] [%X{requestId}] [%thread] %logger{36} : %msg%n"
```
**Example Log Output:**
```text
2026-09-28T10:12:00.827+05:30  INFO [carepath-backend,NONE,NONE] [req-8f2a-4bc1] [main] c.c.service.AuditLogService : [AUDIT_RECORDED] action='LOGIN_SUCCESS', entity='User', id=638cd519-c76d-4da4-8d17-58cfe9d9beec, status='SUCCESS', actor='sarah@carepath.io'
```

---

## 5. Threading, Connection Pooling & Resiliency Guardrails

### 5.1 Asynchronous Thread Pool Isolation
The background notification and alert processing pipeline uses a bounded executor configured in `AsyncConfig.java`:
- **Core Pool Size:** 4 threads
- **Max Pool Size:** 8 threads
- **Queue Capacity:** 100 tasks
- **Rejection Policy:** `ThreadPoolExecutor.CallerRunsPolicy` — ensures backpressure by forcing the submitting thread to execute the task when queues saturate, preventing `RejectedExecutionException` and memory exhaustion.

### 5.2 HikariCP Connection Pool Tuning
- **Maximum Pool Size:** 15 connections
- **Minimum Idle:** 5 connections
- **Connection Timeout:** 20,000 ms
- **Idle Timeout:** 300,000 ms
- **Max Lifetime:** 1,200,000 ms
- **Leak Detection Threshold:** 5,000 ms — automatically emits stack trace alerts if a database connection is borrowed without closure for $>5$ seconds.

---

## 6. Baseline Performance Benchmarks & Service Level Objectives (SLOs)

| Workflow / Endpoint | Target P95 Latency | Target P99 Latency | Availability Target | Max Concurrency |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication (`/api/v1/auth/login`)** | $< 120$ ms | $< 250$ ms | 99.9% | 200 req/s |
| **Vitals Ingestion (`/api/v1/vitals`)** | $< 45$ ms | $< 100$ ms | 99.95% | 500 req/s |
| **ML Risk Prediction (`/ml/v1/risk/predict`)** | $< 60$ ms | $< 120$ ms | 99.9% | 150 req/s |
| **TreeSHAP Explainability (`/ml/v1/risk/explain`)** | $< 180$ ms | $< 350$ ms | 99.5% | 75 req/s |
| **Full Health Checks (`/health/ready`)** | $< 15$ ms | $< 30$ ms | 99.99% | 1000 req/s |

---

## 7. Failure Mode & Effects Analysis (FMEA)

| Failure Scenario | Immediate System Effect | Mitigation & Recovery Strategy |
| :--- | :--- | :--- |
| **PostgreSQL Connectivity Loss** | Backend requests fail with DB connection errors. | Health probe `/health/ready` immediately returns HTTP 503. Container orchestrator halts routing new traffic. HikariCP automatically retries reconnections with exponential backoff. |
| **ML Microservice Unavailability** | Spring Boot calls to `/ml/v1/risk/predict` fail. | Spring `WebClient` circuit breakers and timeouts trip. Frontend falls back to cached baseline metrics and displays descriptive UI warnings without crashing the patient portal. |
| **Notification Queue Saturation** | Async task queue exceeds 100 events. | `CallerRunsPolicy` applies backpressure to the calling thread, slowing ingestion rates to match database write throughput without dropping events. |
| **Rate Limiting Threshold Breached** | Malicious or runaway client sends excessive requests. | In-memory sliding-window filter emits HTTP 429 Too Many Requests with `Retry-After: 60` and logs an audit record to prevent brute-force attacks. |

---

## 8. Multi-Stage Containerization Architecture

CarePath services are containerized using minimal, multi-stage Alpine images following the principle of least privilege:

```
+-----------------------------------------------------------------------------------+
|                               docker-compose.yml                                  |
|                                                                                   |
|  +--------------------+   +-----------------------+   +------------------------+  |
|  |  carepath-postgres |   |  carepath-ml-service  |   |    carepath-backend    |  |
|  |  (postgres:17)     |   |  (python:3.12-slim)   |   | (eclipse-temurin:21-jre)|  |
|  |  Port: 5432        |   |  Port: 8000           |   |  Port: 8080            |  |
|  |  Health: pg_isready|   |  Health: /health/ready|   |  Health: /health/ready |  |
|  +---------^----------+   +-----------^-----------+   +-----------^------------+  |
|            |                          |                           |               |
|            +--------------------------+---------------------------+               |
|                                       |                                           |
|                           +-----------+-----------+                               |
|                           |   carepath-frontend   |                               |
|                           |   (nginx:1.27-alpine) |                               |
|                           |   Port: 3000 -> 80    |                               |
|                           +-----------------------+                               |
+-----------------------------------------------------------------------------------+
```

### 8.1 Build Specifications
- **`backend/Dockerfile`**: Stage 1 uses `maven:3.9-eclipse-temurin-21-alpine` to package the fat JAR. Stage 2 executes the JAR inside `eclipse-temurin:21-jre-alpine` under a non-root `carepath:carepath` user with container memory flags (`-XX:MaxRAMPercentage=75.0`).
- **`frontend/Dockerfile`**: Stage 1 runs `node:20-alpine` to produce Vite distribution bundles. Stage 2 uses `nginx:1.27-alpine` with custom reverse-proxy routing and gzip compression.
- **`ml_service/Dockerfile`**: Hardened `python:3.12-slim` container with healthchecks and model training verification on build.

---

## 9. Automated Quality Gates & Verification Matrix

| Test Suite | Execution Command | Total Tests | Passing | Failures | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Backend Unit & Integration** | `mvnw test -q` | 226 | 226 | 0 | **PASSED** |
| **ML Microservice Pytest** | `pytest tests/` | 54 | 54 | 0 | **PASSED** |
| **Frontend Unit & Component** | `vitest run` | 48 | 48 | 0 | **PASSED** |
| **Frontend Production Build** | `npm run build` | N/A (1920 modules) | Transformed | 0 errors | **PASSED** |
| **End-to-End System Scenario** | `FullSystemE2EIntegrationTest` | Multi-step lifecycle | Verified | 0 | **PASSED** |
| **Frontend E2E Workflows** | `E2EWorkflowScenarios.test.tsx`| 3 major journeys | Verified | 0 | **PASSED** |
| **Docker Compose Config** | `docker compose config` | 4 services | Validated | 0 errors | **PASSED** |

---

## 10. Operational Sign-Off

Phase 11 (Testing, Observability & Production Readiness) has achieved complete technical validation. The platform is ready for controlled deployment and staging verification.
