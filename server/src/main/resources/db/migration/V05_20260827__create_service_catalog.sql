CREATE TABLE service_catalog_items (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    base_price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_service_catalog_items_base_price CHECK (base_price >= 0),
    CONSTRAINT ck_service_catalog_items_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE UNIQUE INDEX uk_service_catalog_items_active_name
    ON service_catalog_items (name)
    WHERE active AND deleted_at IS NULL;
