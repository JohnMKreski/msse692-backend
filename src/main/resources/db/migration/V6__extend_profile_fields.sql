-- VXXX__extend_profiles_fields.sql
-- Extends profiles with type, location, description, socials, websites
-- Safe for repeated runs via IF NOT EXISTS and guarded DO blocks

BEGIN;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS profile_type VARCHAR(16) NOT NULL DEFAULT 'OTHER';

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS location VARCHAR(255);

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS socials JSONB;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS websites JSONB;

-- JSON defaults (app expects arrays)
ALTER TABLE profiles
    ALTER COLUMN socials SET DEFAULT '[]'::jsonb;

ALTER TABLE profiles
    ALTER COLUMN websites SET DEFAULT '[]'::jsonb;

-- 2) Backfill existing rows (only where column exists and is NULL)
-- This will set existing records to OTHER to satisfy NOT NULL constraint
UPDATE profiles
SET profile_type = 'OTHER'
WHERE profile_type IS NULL;

-- 3) Remove column default so future inserts must provide explicit or app-side default
ALTER TABLE profiles
    ALTER COLUMN profile_type DROP DEFAULT;

-- 4) Check constraint for allowed profile_type values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'profiles_profile_type_chk'
          AND conrelid = 'profiles'::regclass
    ) THEN
        ALTER TABLE profiles
            ADD CONSTRAINT profiles_profile_type_chk
            CHECK (profile_type IN ('VENUE','ARTIST','OTHER'));
    END IF;
END
$$;

-- 5) Unique index on user_id (if not already present)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND tablename = 'profiles'
          AND indexname = 'profiles_user_id_uniq'
    ) THEN
        CREATE UNIQUE INDEX profiles_user_id_uniq ON profiles (user_id);
    END IF;
END
$$;

-- 6) Optional: partial index to speed venue location lookups
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND tablename = 'profiles'
          AND indexname = 'profiles_venue_location_idx'
    ) THEN
        CREATE INDEX profiles_venue_location_idx
            ON profiles (location)
            WHERE profile_type = 'VENUE';
    END IF;
END
$$;

-- NOTE: Conditional requirement "location IS NOT NULL WHEN profile_type='VENUE'"
-- is best enforced at application/validation layer, or via a trigger.
-- Pure CHECK constraints for conditional presence across columns are not practical in SQL.

COMMIT;