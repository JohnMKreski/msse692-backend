-- V1: Create base event table for Ark Valley Events

CREATE TABLE event (
    event_id            BIGSERIAL PRIMARY KEY,

    slug                VARCHAR(255) NOT NULL UNIQUE,
    event_name          VARCHAR(255) NOT NULL,
    event_type          VARCHAR(255),

    start_at            TIMESTAMPTZ NOT NULL,
    end_at              TIMESTAMPTZ NOT NULL,

    event_location      VARCHAR(255),

    event_description   TEXT,

    status              VARCHAR(255) NOT NULL DEFAULT 'DRAFT',

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    version             BIGINT
);
