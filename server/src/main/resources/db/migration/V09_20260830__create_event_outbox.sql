ALTER TABLE audit_trail
    ADD COLUMN outbox_event_id UUID,
    ADD COLUMN before_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN after_state JSONB NOT NULL DEFAULT '{}'::jsonb;

CREATE UNIQUE INDEX ux_audit_trail_outbox_event
    ON audit_trail (outbox_event_id)
    WHERE outbox_event_id IS NOT NULL;

CREATE TABLE event_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(60) NOT NULL,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_event_outbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX ix_event_outbox_pending
    ON event_outbox (event_type, status, created_at);
