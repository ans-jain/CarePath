-- =====================================================================
-- CarePath Database Schema Migration: V1__initial_schema.sql
-- Compatible with PostgreSQL 15+
-- =====================================================================

-- 1. Users Table (Core Identity)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Patient Profiles Table
CREATE TABLE patient_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth DATE NOT NULL,
    biological_sex VARCHAR(16) NOT NULL,
    height_cm NUMERIC(5,2) NOT NULL,
    baseline_weight_kg NUMERIC(5,2) NOT NULL,
    smoking_status VARCHAR(32) NOT NULL DEFAULT 'NEVER',
    alcohol_use VARCHAR(32) NOT NULL DEFAULT 'NONE',
    medical_history JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_patient_biological_sex CHECK (biological_sex IN ('MALE', 'FEMALE', 'INTERSEX', 'OTHER')),
    CONSTRAINT chk_patient_height CHECK (height_cm BETWEEN 50.0 AND 260.0),
    CONSTRAINT chk_patient_weight CHECK (baseline_weight_kg BETWEEN 20.0 AND 400.0)
);

-- 3. Clinician Profiles Table
CREATE TABLE clinician_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100) NOT NULL UNIQUE,
    specialty VARCHAR(100) NOT NULL,
    clinic_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Patient-Clinician Delegation Access Table
CREATE TABLE patient_clinician_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    clinician_id UUID NOT NULL REFERENCES clinician_profiles(id) ON DELETE CASCADE,
    access_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    grant_code VARCHAR(64),
    granted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_access_status CHECK (access_status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'REVOKED'))
);

-- 5. Vital Metrics Table (Longitudinal Time-Series)
CREATE TABLE vital_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL,
    metric_type VARCHAR(32) NOT NULL,
    value NUMERIC(8,2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    measurement_context VARCHAR(32) NOT NULL DEFAULT 'RESTING',
    source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_vital_metric_value_positive CHECK (value > 0)
);

-- 6. Symptom Logs Table
CREATE TABLE symptom_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL,
    symptom_type VARCHAR(64) NOT NULL,
    severity_score INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_symptom_severity CHECK (severity_score BETWEEN 1 AND 10)
);

-- 7. Patient Baselines Table
CREATE TABLE patient_baselines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    metric_type VARCHAR(32) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    mean_value NUMERIC(8,2) NOT NULL,
    median_value NUMERIC(8,2) NOT NULL,
    std_deviation NUMERIC(8,2) NOT NULL,
    p25_value NUMERIC(8,2) NOT NULL,
    p75_value NUMERIC(8,2) NOT NULL,
    ewma_value NUMERIC(8,2) NOT NULL,
    sample_count INT NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. Risk Assessments Table
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    assessment_timestamp TIMESTAMPTZ NOT NULL,
    model_version VARCHAR(32) NOT NULL,
    overall_risk_score NUMERIC(4,3) NOT NULL,
    risk_category VARCHAR(20) NOT NULL,
    confidence_level VARCHAR(32) NOT NULL,
    feature_snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_risk_score_bounds CHECK (overall_risk_score BETWEEN 0.000 AND 1.000),
    CONSTRAINT chk_risk_category CHECK (risk_category IN ('LOW', 'MODERATE', 'ELEVATED', 'HIGH')),
    CONSTRAINT chk_confidence_level CHECK (confidence_level IN ('PRELIMINARY_INTAKE', 'LONGITUDINAL_ROBUST'))
);

-- 9. SHAP Explanations Table
CREATE TABLE shap_explanations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_assessment_id UUID NOT NULL UNIQUE REFERENCES risk_assessments(id) ON DELETE CASCADE,
    base_value NUMERIC(6,4) NOT NULL,
    feature_contributions JSONB NOT NULL,
    top_risk_drivers JSONB NOT NULL,
    top_protective_factors JSONB NOT NULL,
    clinical_summary_narrative TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. Counterfactual Recommendations Table
CREATE TABLE counterfactual_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_assessment_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    target_risk_score NUMERIC(4,3) NOT NULL,
    simulated_features JSONB NOT NULL,
    feasibility_distance NUMERIC(6,3) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Clinical Alerts Table
CREATE TABLE clinical_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    alert_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    trigger_metric_id UUID REFERENCES vital_metrics(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    acknowledged_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'URGENT', 'CRITICAL')),
    CONSTRAINT chk_alert_status CHECK (status IN ('NEW', 'ACKNOWLEDGED', 'RESOLVED'))
);

-- 12. Audit Logs Table (Immutable Audit Ledger)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    target_patient_id UUID,
    action_type VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id UUID,
    ip_address VARCHAR(45) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Performance & Analytical Indexes (as defined in DATABASE_DESIGN.md)
-- =====================================================================

-- Time-Series Vital Metric Lookup
CREATE INDEX idx_vitals_patient_metric_time 
ON vital_metrics (patient_id, metric_type, recorded_at DESC);

CREATE INDEX idx_vitals_patient_time 
ON vital_metrics (patient_id, recorded_at DESC);

-- Baseline Analysis Lookup
CREATE INDEX idx_baselines_patient_metric 
ON patient_baselines (patient_id, metric_type, window_end DESC);

-- Longitudinal Risk Assessment Trajectory
CREATE INDEX idx_risk_patient_time 
ON risk_assessments (patient_id, assessment_timestamp DESC);

-- Clinical Alerts Dashboard Triage
CREATE INDEX idx_alerts_patient_status_sev 
ON clinical_alerts (patient_id, status, severity, created_at DESC);

-- Clinician Delegation Access
CREATE INDEX idx_access_clinician_patient_status 
ON patient_clinician_access (clinician_id, patient_id, access_status);

-- Compliance Audit Log Inquiries
CREATE INDEX idx_audit_target_patient_time 
ON audit_logs (target_patient_id, created_at DESC);
