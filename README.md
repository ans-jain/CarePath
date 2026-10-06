# CarePath — Longitudinal Health Monitoring & Explainable Risk Analysis Platform

**CarePath** is an enterprise-grade clinical decision-support platform providing longitudinal biomarker monitoring, calibrated cardiometabolic risk stratification, TreeSHAP explainability attributions, and constrained counterfactual lifestyle recommendations.

> [!IMPORTANT]
> **MANDATORY CLINICAL GUARDRAIL**: CarePath operates strictly as a decision-support risk signal and feature attribution platform. It does **NOT** diagnose medical conditions, prescribe medication, or replace a licensed medical practitioner. All risk scores and attributions are non-diagnostic decision-support aids.

---

## 1. System Architecture

CarePath is implemented as a multi-tier clean architecture:

```
                            +-------------------------------------------+
                            |               Client Browser              |
                            +---------------------+---------------------+
                                                  |
                                            HTTPS / HTTP
                                                  |
                            +---------------------v---------------------+
                            |    carepath-frontend (NGINX 1.27 Edge)    |
                            |  - Port 80 (Public)                       |
                            |  - Static React SPA Delivery              |
                            |  - Reverse Proxies: /api/ and /ml/        |
                            +----------+--------------------+-----------+
                                       |                    |
                                 Internal Proxy        Internal Proxy
                                       |                    |
         +-----------------------------v----+        +------v--------------------------+
         |   carepath-backend (Spring Boot) |        |  carepath-ml-service (FastAPI)  |
         | - Java 21 / Eclipse Temurin      |        | - Python 3.12 / scikit-learn    |
         | - Port 8080 (Internal)           |        | - TreeSHAP & Counterfactuals    |
         | - JWT, RBAC & Vitals Engine      |        | - Port 8000 (Internal)          |
         | - Probes: /health, /health/ready |        | - Probes: /ml/v1/health/ready   |
         +--------------+-------------------+        +---------------------------------+
                        |
                 PostgreSQL JDBC
                        |
         +--------------v-------------------+
         |      carepath-postgres           |
         | - PostgreSQL 17 Alpine           |
         | - Port 5432 (Internal)           |
         | - Flyway Migrations (V1-V3)      |
         +----------------------------------+
```

---

## 2. Local Development Setup

### 2.1 Prerequisites
- **Java**: JDK 21+ (Eclipse Temurin recommended)
- **Node.js**: v20+ & npm 10+
- **Python**: 3.12+
- **Docker**: Docker Engine 24+ & Docker Compose v2+

### 2.2 Environment Configuration
Clone the repository and copy the environment template:
```bash
cp .env.example .env
```
Ensure `.env` contains your local database credentials and a 256-bit `JWT_SECRET`.

### 2.3 Starting Services with Docker Compose (Local Stack)
To run the entire local development stack with hot-reloading:
```bash
docker compose up -d
```
Service entrypoints:
- **Web Frontend**: `http://localhost:3000`
- **Spring Boot Backend**: `http://localhost:8080` (Health: `http://localhost:8080/health`)
- **FastAPI ML Service**: `http://localhost:8000` (Health: `http://localhost:8000/ml/v1/health`)
- **PostgreSQL Database**: `localhost:5432` (`carepath` / `postgres`)

### 2.4 Running Services Locally (Bare Metal)

#### Backend (Spring Boot 3.4.3):
```bash
cd backend
./mvnw clean spring-boot:run
```

#### ML Microservice (FastAPI):
```bash
cd ml_service
python -m venv .venv
source .venv/bin/activate  # Or on Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

#### Frontend (React 18 / Vite):
```bash
cd frontend
npm install
npm run dev
```

---

## 3. Running Automated Test Suites

CarePath maintains an automated test verification matrix with 328+ automated tests:

### 3.1 Backend Tests (226 Tests)
```bash
cd backend
./mvnw clean test
```
*Generates JaCoCo code coverage report in `target/site/jacoco/index.html`.*

### 3.2 ML Microservice Tests (54 Tests)
```bash
cd ml_service
python -m pytest tests/ -v
```

### 3.3 Frontend Tests (48 Tests across 11 Suites)
```bash
cd frontend
npm test
```

### 3.4 Frontend Production Build Verification
```bash
cd frontend
npm run build
```

---

## 4. Production Deployment

### 4.1 Production Architecture & Security Guardrails
In production:
1. **Network Isolation**: The PostgreSQL database and ML microservice do **not** publish ports to the public host. All inter-service traffic routes through the internal Docker bridge network (`carepath-internal-network`).
2. **Edge Reverse Proxy**: The NGINX frontend container serves as the secure reverse proxy on port 80/443, routing `/api/` traffic to the backend and `/ml/` traffic to the ML service.
3. **Least Privilege**: The backend runs as a non-root application user (`carepath:carepath`) inside Alpine containers.
4. **Deterministic ML Artifacts**: Pre-trained model artifacts (`model.joblib`, `preprocessor.joblib`, `model_metadata.json`) are version-packaged. **No retraining occurs at deployment time.**
5. **Database Migrations**: Flyway runs automatically on startup, migrating the PostgreSQL database from a clean state through `V1`, `V2`, and `V3` without data loss.

### 4.2 Production Deployment Command
```bash
# 1. Provide production credentials via .env or platform secrets
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

### 4.3 Production Smoke Test
Verify deployment health and end-to-end functionality using the automated smoke test script:
```bash
python scripts/smoke_test.py --base-url http://localhost:80 --backend-url http://localhost:8080 --ml-url http://localhost:8000
```
This tests:
- Core service `/health` and deep `/health/ready` database connectivity.
- ML service `/ml/v1/health` and `/ml/v1/health/ready` artifact loading state.
- Authentication guards and login token issuance.
- ML calibrated risk predictions ($0.00 - 1.00$).
- TreeSHAP feature attributions and counterfactual optimization plans.

---

## 5. Continuous Integration & CD Pipeline

The automated CI workflow is configured under `.github/workflows/ci.yml`:

```
                       Push / Pull Request
                                │
         +----------------------+----------------------+
         │                      │                      │
         ▼                      ▼                      ▼
   [backend-ci]          [ml-service-ci]         [frontend-ci]
   - Java 21             - Python 3.12           - Node 20
   - Postgres Service    - Model Verification    - TypeScript Check
   - 226 Unit/Int Tests  - 54 Pytest Tests       - 48 Vitest Tests
   - JaCoCo Coverage     - TreeSHAP & Boundaries - Production Build
         │                      │                      │
         +----------------------+----------------------+
                                │
                                ▼
                       [docker-build-ci]
                       - Compose Config Check
                       - Docker Multi-Stage Builds
                       - Image Artifact Validation
```

---

## 6. Rollback & Disaster Recovery Procedures

1. **Application Container Rollback**:
   If a container failure occurs, revert to the previous container image tag:
   ```bash
   docker compose -f docker-compose.prod.yml down
   docker compose -f docker-compose.prod.yml up -d
   ```
2. **Database Rollback Strategy**:
   Flyway migrations in `V1`-`V3` are strictly additive and backward-compatible (non-destructive `ADD COLUMN IF NOT EXISTS`). Database snapshots must be taken prior to major schema version bumps.
3. **ML Model Rollback**:
   Model artifacts are version-controlled in `ml_service/app/artifacts/`. To roll back a model version, checkout the previous artifact commit; container verification immediately validates the load state.

---

## 7. Documentation Index

- [`docs/PRODUCT_REQUIREMENTS.md`](file:///c:/Users/Aditi/OneDrive/Desktop/Projects/CarePath/docs/PRODUCT_REQUIREMENTS.md): Clinical guardrails and platform specifications.
- [`docs/ARCHITECTURE.md`](file:///c:/Users/Aditi/OneDrive/Desktop/Projects/CarePath/docs/ARCHITECTURE.md): Multi-tier architecture blueprint.
- [`docs/DATABASE_DESIGN.md`](file:///c:/Users/Aditi/OneDrive/Desktop/Projects/CarePath/docs/DATABASE_DESIGN.md): PostgreSQL schemas and indexes.
- [`docs/SECURITY.md`](file:///c:/Users/Aditi/OneDrive/Desktop/Projects/CarePath/docs/SECURITY.md): RBAC, JWT, and audit logging.
- [`docs/PRODUCTION_READINESS.md`](file:///c:/Users/Aditi/OneDrive/Desktop/Projects/CarePath/docs/PRODUCTION_READINESS.md): Production checklist, SLA benchmarks, and FMEA analysis.
- [`docs/DEPLOYMENT.md`](file:///c:/Users/Aditi/OneDrive/Desktop/Projects/CarePath/docs/DEPLOYMENT.md): Detailed production deployment and operations guide.

## Author

**Anshika**
