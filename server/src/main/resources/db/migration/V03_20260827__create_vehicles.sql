CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    license_plate VARCHAR(64) NOT NULL,
    make VARCHAR(60) NOT NULL,
    model VARCHAR(60) NOT NULL,
    model_year INTEGER NOT NULL,
    customer_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_vehicles_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_vehicles_model_year CHECK (model_year BETWEEN 1886 AND 9999),
    CONSTRAINT ck_vehicles_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE UNIQUE INDEX uk_vehicles_active_license_plate
    ON vehicles (license_plate)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_vehicles_customer_id ON vehicles (customer_id);
