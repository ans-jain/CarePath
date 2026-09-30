# CarePath - Autonomous Agent Operating Guidelines (AGENTS.md)

## 1. Project Mission & Identity
CarePath is an enterprise-grade, longitudinal health-monitoring and explainable risk-analysis platform.
**CRITICAL CLINICAL GUARDRAIL**: CarePath must **NOT** diagnose diseases, prescribe medication, or claim to replace doctors. Model outputs must be presented strictly as decision-support risk signals and feature attributions.

## 2. Architectural Blueprint Reference
All development must strictly adhere to the technical specification documents in `docs/`:
- `docs/PRODUCT_REQUIREMENTS.md` - Clinical guardrails, personas, functional & non-functional requirements.
- `docs/ARCHITECTURE.md` - System architecture, tier boundaries, security model, and data flows.
- `docs/DATABASE_DESIGN.md` - PostgreSQL schema, entity definitions, constraints, and indexes.
- `docs/API_DESIGN.md` - REST API contracts, DTO schemas, and error responses.
- `docs/ML_DESIGN.md` - Feature taxonomy, calibrated models, TreeSHAP explainer, and counterfactual solver.
- `docs/DEVELOPMENT_ROADMAP.md` - Phase-by-phase dependency graph and testing gates.

## 3. Engineering Rules & Phase Control
1. **Phased Execution Only**: Never jump ahead or implement future phases prematurely.
2. **Phase 1 Boundary**:
   - Scope is strictly: PostgreSQL integration, JPA configuration, Core Entities, Entity Relationships, Repositories, Database Schema, and Database Tests.
   - Prohibited in Phase 1: Security/JWT filters, Auth controllers, Patient REST APIs, ML microservice code, React UI, Notification brokers, Report PDF generators.
3. **Data Integrity & Conventions**:
   - Primary Keys: UUIDs (`java.util.UUID`).
   - Timestamps: UTC `java.time.Instant` or `java.time.LocalDate`.
   - Relationships: Proper JPA mappings (`@ManyToOne(fetch = FetchType.LAZY)`, `@OneToOne`, avoiding circular JSON/toString references).
   - Enums: Explicit string mappings (`@Enumerated(EnumType.STRING)`).
   - Validation: Bean validation annotations (`@NotNull`, `@Size`, `@Min`, `@Max`).
   - Clean Architecture: Keep business logic out of entities; keep REST controllers and services out of Phase 1 unless needed for testing.
4. **Environment & Security**:
   - Never hardcode passwords or credentials in source code.
   - Use Spring profiles and environment variable overrides (`${DB_USERNAME:postgres}`, `${DB_PASSWORD:postgres}`).
