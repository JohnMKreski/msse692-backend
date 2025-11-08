-- V4: Add created_by and last_modified_by to events, plus event_audit table
-- Assumptions:
--  - events table already exists (uses ID column event_id primary key)
--  - app_users table exists with primary key id

-- 1. Add ownership columns to events
-- Using separate ALTER statements for broader DB compatibility (H2/Postgres)
ALTER TABLE event ADD created_by BIGINT;
ALTER TABLE event ADD last_modified_by BIGINT;

-- 2. Add foreign key constraints (defer if existing rows violate; using NO ACTION for now)
ALTER TABLE event ADD CONSTRAINT fk_event_created_by FOREIGN KEY (created_by) REFERENCES app_users (id);
ALTER TABLE event ADD CONSTRAINT fk_event_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES app_users (id);

-- 3. Create event_audit table
CREATE TABLE event_audit (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE
    at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    snapshot JSONB,              -- optional full snapshot of event after action
    CONSTRAINT fk_event_audit_event FOREIGN KEY (event_id) REFERENCES event (event_id) ON DELETE CASCADE,
    CONSTRAINT fk_event_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_users (id)
);

-- 4. Indexes for lookup performance
CREATE INDEX idx_event_created_by ON event (created_by);
CREATE INDEX idx_event_last_modified_by ON event (last_modified_by);
CREATE INDEX idx_event_audit_event_id ON event_audit (event_id);
CREATE INDEX idx_event_audit_actor_user_id ON event_audit (actor_user_id);

-- 5. (Optional) Backfill: set created_by = last_modified_by for existing events using a placeholder system user id if known.
-- UPDATE event SET created_by = <system_user_id>, last_modified_by = <system_user_id> WHERE created_by IS NULL;
-- (Leave commented; handle via manual migration if needed.)
