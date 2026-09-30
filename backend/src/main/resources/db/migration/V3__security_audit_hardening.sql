-- =====================================================================
-- Phase 10: Security Hardening & Audit Logging Migration
-- =====================================================================

-- 1. Allow nullable actor_user_id for unauthenticated attempts (e.g. failed logins)
ALTER TABLE audit_logs ALTER COLUMN actor_user_id DROP NOT NULL;

-- 2. Add actor_email, user_agent, and status columns to audit_logs
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS actor_email VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';

-- 3. Composite and analytical indexes for administrative querying and compliance review
CREATE INDEX IF NOT EXISTS idx_audit_actor_created ON audit_logs (actor_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_created ON audit_logs (action_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_logs (status);
