-- V5__role_requests.sql
-- Creates role_requests + role_request_roles tables for Phase 1 role elevation workflow.
-- NOTE: IF NOT EXISTS hides mistakes if a mismatched table already exists.

-- Flyway runs migrations in a transaction for PostgreSQL by default.
-- BEGIN/COMMIT optional; omit if you prefer Flyway’s default behavior.

CREATE TABLE IF NOT EXISTS role_requests (
    id              VARCHAR(36) PRIMARY KEY,          -- UUID stored as string (generated in application)
    requester_uid   VARCHAR(128) NOT NULL,            -- Firebase UID of requesting user
    reason          VARCHAR(500),                     -- Optional request reason
    status          VARCHAR(20) NOT NULL,             -- PENDING / APPROVED / REJECTED / CANCELED
    approver_uid    VARCHAR(128),                     -- UID of admin who approved/rejected
    approver_note   VARCHAR(500),                     -- Optional note from approver
    created_at      TIMESTAMPTZ NOT NULL,             -- Set in @PrePersist
    decided_at      TIMESTAMPTZ,                      -- Set when approved/rejected (null otherwise)
    version         BIGINT,                           -- Optimistic lock (@Version)
    CONSTRAINT fk_role_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELED'))
);

-- ElementCollection join table for requestedRoles (uppercase normalized in service/mapper).
-- Join table storing one row per requested role; enables future multi-role requests
-- while keeping each request a single auditable approval decision.
CREATE TABLE IF NOT EXISTS role_request_roles (
    role_request_id VARCHAR(36) NOT NULL,
    role            VARCHAR(50) NOT NULL,
    PRIMARY KEY (role_request_id, role),
    CONSTRAINT fk_role_request_roles_request
        FOREIGN KEY (role_request_id)
        REFERENCES role_requests (id)
        ON DELETE CASCADE
);

-- Index to support listing by status and recent creation time (descending).
-- DESC accepted by PostgreSQL; if another DB is used, adjust accordingly.
CREATE INDEX IF NOT EXISTS idx_role_requests_status_created_at
    ON role_requests (status, created_at DESC);

-- Index to support filtering by requester and status for user history or admin UI.
CREATE INDEX IF NOT EXISTS idx_role_requests_requester_status
    ON role_requests (requester_uid, status);

-- Optional narrow index for requester-only queries (list all requests for a user).
CREATE INDEX IF NOT EXISTS idx_role_requests_requester
    ON role_requests (requester_uid);

-- Optional index for status-only dashboards.
CREATE INDEX IF NOT EXISTS idx_role_requests_status
    ON role_requests (status);