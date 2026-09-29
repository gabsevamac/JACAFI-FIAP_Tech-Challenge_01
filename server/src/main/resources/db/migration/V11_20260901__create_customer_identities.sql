CREATE TABLE customer_identities (
    subject_id VARCHAR(64) PRIMARY KEY,
    customer_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_identities_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_customer_identities_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE UNIQUE INDEX uk_customer_identities_customer ON customer_identities (customer_id);

INSERT INTO customer_identities (
    subject_id, customer_id,
    created_at, created_by, updated_at, updated_by, version
)
SELECT id::text, customer_id, CURRENT_TIMESTAMP, 'flyway', CURRENT_TIMESTAMP, 'flyway', 0
FROM user_accounts
WHERE customer_id IS NOT NULL
  AND deleted_at IS NULL;
