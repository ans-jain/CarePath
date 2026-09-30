-- =====================================================================
-- Phase: Doctor Account Status & Verification
-- Migration: V4__doctor_account_status.sql
-- =====================================================================

-- 1. Add status column to users table if not exists (PENDING, ACTIVE, REJECTED)
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

-- 2. Ensure existing users have ACTIVE status
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;

-- 3. Add index on role and status for efficient administrative triage
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users (role, status);
