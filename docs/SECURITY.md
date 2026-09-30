# CarePath Security Architecture & Hardening Guide (SECURITY.md)

## 1. Executive Summary & Security Philosophy

CarePath is an enterprise-grade longitudinal health-monitoring and clinical explainable risk-analysis platform. Because it processes protected health information (PHI) and physiological telemetry, security is engineered as a foundational, defense-in-depth architecture adhering to HIPAA, GDPR, and OWASP Top 10 recommendations.

### Core Security Tenets
1. **Principle of Least Privilege (PoLP)**: Users, clinicians, and background services operate with the minimal permissions required for their specific function.
2. **Explicit Relationship-Based Access (ReBAC)**: Clinicians cannot view patient records without an explicit, active delegation grant (`PatientClinicianAccess` with status `ACTIVE`).
3. **Immutable Auditability**: All security-relevant actions, authentication attempts, access violations, and data mutations produce an append-only audit trail scrubbed of sensitive secrets and passwords.
4. **Defense in Depth**: Security controls are applied across network boundaries, HTTP transport headers, rate-limiting filters, Spring Security method-level annotations (`@PreAuthorize`), and database constraints.
5. **Strict Clinical Guardrail**: Model outputs are strictly clinical decision-support risk signals. No diagnostic claims or prescriptive decisions are made by automated agents or ML microservices.

---

## 2. Role-Based Access Control (RBAC) Matrix

CarePath enforces three distinct system roles defined in `com.carepath.domain.enums.Role`:

| Endpoint / Operation | ROLE_PATIENT | ROLE_CLINICIAN | ROLE_ADMIN | Access Control Enforcement Mechanism |
| :--- | :---: | :---: | :---: | :--- |
| **Self Registration (`/api/v1/auth/register`)** | Allowed (Fixed to Patient) | N/A (Admin provisioned) | N/A (Admin provisioned) | `AuthService.register()` enforces `ROLE_PATIENT`; prevents privilege self-escalation |
| **Authentication (`/api/v1/auth/login`)** | Own Account | Own Account | Own Account | BCrypt + Account Active Check (`is_active = true`) |
| **Own Profile (`/api/v1/patients/me`)** | Own Profile | Forbidden (Clinicians use doctor endpoints) | Global Read | `AccessControlService.checkPatientAccess()` |
| **Specific Patient Profile (`/api/v1/patients/{id}`)** | Own Profile Only | Assigned Patients Only (`ACTIVE` grant) | Global Access | `AccessControlService.checkPatientAccess()` + Audit Log |
| **Vitals Ingestion (`POST /api/v1/vitals`)** | Own Telemetry | Assigned Patients Only | Forbidden (Audited) | `VitalMetricService` + `PhysiologicalRangeValidator` |
| **Vitals Retrieval (`GET /api/v1/vitals/**`)** | Own Telemetry | Assigned Patients Only | Global Read | `VitalMetricService` ownership verification |
| **Risk Assessment (`/api/v1/risk-assessments/**`)** | Own Records | Assigned Patients Only | Global Read | `RiskAssessmentService` + `AccessControlService` |
| **Notifications & Preferences** | Own Notifications | Own Notifications | Global Management | `NotificationService` user verification |
| **Admin Console (`/api/v1/admin/**`)** | Forbidden (403) | Forbidden (403) | Full Control | Spring Security `@PreAuthorize("hasRole('ADMIN')")` |
| **Audit Log Querying** | Forbidden (403) | Forbidden (403) | Full Control | `AdminController.getAuditLogs()` |

### Privilege Self-Escalation Prevention
The public self-registration endpoint (`/api/v1/auth/register`) strictly sets the newly registered user's role to `Role.ROLE_PATIENT`, ignoring any role specified in the client request payload. Promotion to `ROLE_CLINICIAN` or `ROLE_ADMIN` can only be performed by an existing administrator through `/api/v1/admin/users/{userId}/role`, and is logged as a high-priority `ROLE_CHANGE` audit event.

---

## 3. Clinician Access Delegation Model

A fundamental security requirement is that clinicians do **not** possess blanket access to all patients in the system:
1. When a clinician attempts to view a patient's vitals, profile, or risk assessments, `AccessControlService` inspects `ClinicianProfile` and `PatientClinicianAccessRepository`.
2. Access is granted **only** if:
   - A `PatientClinicianAccess` record exists linking the target patient and the clinician's profile.
   - The access record has `status = AccessStatus.ACTIVE`.
   - The current time does not exceed the optional delegation expiration window (`expiresAt`).
3. If an unassigned clinician attempts to query patient records:
   - Access is immediately terminated with HTTP `403 Forbidden`.
   - An immutable audit log entry is recorded with action `FORBIDDEN_ACCESS_ATTEMPT`, capturing the clinician's email, target patient ID, IP address, and user agent.

---

## 4. Immutable Audit Logging

Audit logs are stored in the PostgreSQL `audit_logs` table via `com.carepath.domain.models.AuditLog` and `com.carepath.service.AuditLogService`.

### Audit Schema
- `id`: UUID (Primary Key)
- `action_type`: VARCHAR(64) NOT NULL (Indexed for rapid filtering)
- `actor_user_id`: UUID NULL (Foreign key to `users(id)`, nullable for unauthenticated or external attempts)
- `actor_email`: VARCHAR(255) NULL (Captures principal email at the moment of the event)
- `entity_name`: VARCHAR(64) NULL (e.g. `PatientProfile`, `VitalMetric`, `User`)
- `entity_id`: VARCHAR(64) NULL (Target entity UUID)
- `target_patient_id`: UUID NULL (Patient context for clinical audits)
- `details`: TEXT NULL (Sanitized event description)
- `ip_address`: VARCHAR(45) NULL (IPv4 or IPv6 client origin)
- `user_agent`: VARCHAR(255) NULL (Client application identifier)
- `status`: VARCHAR(32) NOT NULL (e.g. `SUCCESS`, `FAILURE`, `FORBIDDEN`)
- `created_at`: TIMESTAMP WITH TIME ZONE (UTC, Indexed)

### Credential & PHI Sanitization
Before audit logs are committed to persistent storage, `AuditLogService.sanitizeDetails()` strips sensitive key-value patterns:
- Passwords (`password=...`, `"password": "..."`)
- Authorization headers & Bearer tokens (`Bearer eyJ...`, `token=...`)
- API keys & secrets (`secret=...`, `apiKey=...`)

### Admin-Only Access & Non-Repudiation
Audit log records cannot be updated or deleted through any application REST API. Only administrators (`ROLE_ADMIN`) may inspect the audit trail via `/api/v1/admin/audit-logs`, which supports dynamic multi-criteria filtering (action type, status, free-text search, pagination).

---

## 5. Rate Limiting & Denial of Service (DoS) Defense

The CarePath backend incorporates an in-memory sliding-window rate limiter implemented as `com.carepath.security.RateLimitingFilter`:

### Configurable Thresholds
- **Authentication Endpoints (`/api/v1/auth/**`)**: `10 requests/minute` per IP address. Prevents brute-force credential stuffing and password-guessing attacks.
- **Machine Learning Inference (`/ml/**`, `/api/v1/risk-assessments/evaluate`)**: `30 requests/minute` per IP address. Protects CPU/memory-intensive gradient boosting and SHAP tree traversal microservices.
- **General API Endpoints (`/api/v1/**`)**: `100 requests/minute` per IP address. Prevents telemetry scraping and excessive polling.

### Rejection Behavior
When an origin exceeds its sliding-window quota:
- The filter rejects the request immediately with HTTP `429 Too Many Requests`.
- Standard RFC 6585 header `Retry-After: 60` is emitted.
- A standardized JSON error payload is returned:
  ```json
  {
    "status": 429,
    "error": "Too Many Requests",
    "message": "Rate limit exceeded. Please retry after 60 seconds."
  }
  ```

---

## 6. HTTP Security Headers & Transport Security

In `com.carepath.security.SecurityConfig`, Spring Security is configured with industry-standard hardened headers:

1. **Content Security Policy (CSP)**:
   `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; frame-ancestors 'none';`
   Prevents cross-site scripting (XSS) and unauthorized resource execution.
2. **HTTP Strict Transport Security (HSTS)**:
   `max-age=31536000; includeSubDomains; preload`
   Forces clients to communicate exclusively over TLS/HTTPS.
3. **MIME-Type Sniffing Protection**:
   `X-Content-Type-Options: nosniff`
   Blocks browser MIME-type confusion attacks.
4. **Clickjacking Defense**:
   `X-Frame-Options: DENY`
   Prohibits rendering the platform in frames or iframes.
5. **Referrer Policy**:
   `Referrer-Policy: strict-origin-when-cross-origin`
   Ensures sensitive path parameters and tokens are not leaked in HTTP referrer headers.

---

## 7. Cross-Origin Resource Sharing (CORS) Policy

CORS is strictly governed to prevent cross-site request hijacking:
- **Allowed Origins**: Configured via the `app.cors.allowed-origins` property (or `APP_CORS_ALLOWED_ORIGINS` environment variable).
- **Wildcard Policy**: Wildcard origins (`*`) are strictly prohibited in combination with credentials (`allowCredentials(true)`).
- **Allowed Methods**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- **Max Age**: Pre-flight cache duration is set to `3600 seconds` (1 hour) to reduce unnecessary OPTIONS requests while maintaining strict origin checks.

---

## 8. Centralized Exception & Information Disclosure Protection

CarePath routes all application and framework errors through `com.carepath.api.exception.GlobalExceptionHandler`:
- **RFC 7807 Problem Details**: Consistent, client-friendly error structures (`timestamp`, `status`, `error`, `message`, `path`).
- **Zero Stack Trace Leakage**: Internal database vendor errors, SQL queries, Hibernate mapping details, and Java stack traces are never exposed in HTTP response bodies.
- **Detailed Server Logs**: Unhandled exceptions are logged internally at `ERROR` level with correlated trace IDs for administrator diagnostic inspection.

---

## 9. Environment & Deployment Security Matrix

| Parameter / Secret | Development Default | Production Required Value | Notes |
| :--- | :--- | :--- | :--- |
| `DB_USERNAME` | `postgres` | Custom role-restricted DB user | Do not use postgres superuser in prod |
| `DB_PASSWORD` | `postgres` | High-entropy vault-injected secret | Never commit to source code |
| `JWT_SECRET` | `carepath-dev-super-secret-jwt-key...` | Cryptographically random $\ge 256$-bit secret | Rotate periodically |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Strict production domains (`https://app.carepath.io`) | Comma-separated |
| `APP_RATE_LIMIT_ENABLED` | `true` | `true` | Disable only in isolated micro-benchmarks |
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` | Enables optimized connection pooling |

---

## 10. Security Verification & Test Coverage

The security hardening suite is validated through dedicated automated unit and integration tests:
- `AccessControlServiceTest`: Validates patient ownership verification, active clinician relationship checking, unassigned clinician blocking, and admin override.
- `AuditLogServiceTest`: Validates credential and secret scrubbers, log recording, and specification query filtering.
- `RateLimitingFilterTest`: Validates sliding window quota consumption, per-endpoint limits, and HTTP 429 response generation.
- `AuthHardeningSecurityTest`: Validates self-registration role forcing (`ROLE_PATIENT`) and invalid credential failure logging.
- `AdminControllerIntegrationTest`: Validates Spring Security role enforcement on `/api/v1/admin/**` (200 OK for ADMIN, 403 Forbidden for PATIENT/CLINICIAN).
