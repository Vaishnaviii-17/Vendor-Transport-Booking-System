
-- Task 7: Align vehicles table with Vehicle entity

ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
