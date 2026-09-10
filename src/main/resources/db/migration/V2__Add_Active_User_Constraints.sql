-- Add functional indexes to ensure uniqueness among active users only.
-- In MySQL, multiple NULL values in a UNIQUE constraint are treated as distinct.
-- By using IFNULL with a default epoch, we guarantee that multiple active users (deleted_at is NULL)
-- with the same phone or email will violate the unique constraint.

CREATE UNIQUE INDEX idx_unique_active_phone ON users (phone, (IFNULL(deleted_at, '1970-01-01 00:00:00.000000')));
CREATE UNIQUE INDEX idx_unique_active_email ON users (email, (IFNULL(deleted_at, '1970-01-01 00:00:00.000000')));
