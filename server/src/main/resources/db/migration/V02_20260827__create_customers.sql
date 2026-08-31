CREATE TABLE customers (
    id UUID PRIMARY KEY,
    tax_id VARCHAR(14) NOT NULL,
    name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150),
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_customers_tax_id_shape
        CHECK (tax_id ~ '^[0-9]{11}$' OR tax_id ~ '^[A-Z0-9]{12}[0-9]{2}$'),
    CONSTRAINT ck_customers_trade_name
        CHECK (length(tax_id) = 14 OR trade_name IS NULL),
    CONSTRAINT ck_customers_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE UNIQUE INDEX uk_customers_tax_id ON customers (tax_id);

ALTER TABLE user_accounts
    ADD CONSTRAINT fk_user_accounts_customer
    FOREIGN KEY (customer_id) REFERENCES customers (id);

CREATE UNIQUE INDEX uk_user_accounts_customer
    ON user_accounts (customer_id)
    WHERE customer_id IS NOT NULL;
