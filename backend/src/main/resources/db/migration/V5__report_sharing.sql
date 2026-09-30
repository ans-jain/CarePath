-- =====================================================================
-- Phase: Report Sharing & Doctor Clinical Access
-- Migration: V5__report_sharing.sql
-- =====================================================================

-- 1. Create report_shares table linking risk_assessments, patient_profiles, and doctor users
CREATE TABLE IF NOT EXISTS report_shares (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    viewed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_report_doctor UNIQUE (report_id, doctor_id)
);

-- 2. Indexes for efficient lookup
CREATE INDEX IF NOT EXISTS idx_report_shares_doctor_id ON report_shares(doctor_id);
CREATE INDEX IF NOT EXISTS idx_report_shares_patient_id ON report_shares(patient_id);
CREATE INDEX IF NOT EXISTS idx_report_shares_report_id ON report_shares(report_id);
CREATE INDEX IF NOT EXISTS idx_report_shares_status ON report_shares(status);
