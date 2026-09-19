-- Add reset_at column to auth_codes to record when a reset token has been consumed, preventing token reuse
ALTER TABLE auth_codes ADD COLUMN reset_at DATETIME(6) NULL;
