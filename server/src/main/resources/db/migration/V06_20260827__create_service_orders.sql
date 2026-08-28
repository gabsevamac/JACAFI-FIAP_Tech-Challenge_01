CREATE TABLE service_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    status VARCHAR(25) NOT NULL,
    reported_issue TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_service_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_service_orders_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT ck_service_orders_status CHECK (status IN (
        'RECEIVED', 'UNDER_DIAGNOSIS', 'AWAITING_APPROVAL',
        'IN_PROGRESS', 'COMPLETED', 'DELIVERED'
    )),
    CONSTRAINT ck_service_orders_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE INDEX ix_service_orders_customer ON service_orders (customer_id);
CREATE INDEX ix_service_orders_vehicle ON service_orders (vehicle_id);

CREATE INDEX ix_service_orders_operational_queue
    ON service_orders (
        (CASE status
            WHEN 'IN_PROGRESS' THEN 1
            WHEN 'AWAITING_APPROVAL' THEN 2
            WHEN 'UNDER_DIAGNOSIS' THEN 3
            WHEN 'RECEIVED' THEN 4
        END),
        created_at,
        id
    )
    WHERE status IN ('IN_PROGRESS', 'AWAITING_APPROVAL', 'UNDER_DIAGNOSIS', 'RECEIVED')
      AND deleted_at IS NULL;

CREATE TABLE service_order_service_lines (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL,
    service_catalog_item_id UUID NOT NULL,
    service_name_snapshot VARCHAR(120) NOT NULL,
    unit_price_snapshot NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_service_order_service_lines_order
        FOREIGN KEY (service_order_id) REFERENCES service_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_order_service_lines_catalog_item
        FOREIGN KEY (service_catalog_item_id) REFERENCES service_catalog_items (id),
    CONSTRAINT ck_service_order_service_lines_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT ck_service_order_service_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_service_order_service_lines_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE INDEX ix_service_order_service_lines_order
    ON service_order_service_lines (service_order_id);

CREATE TABLE service_order_material_lines (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    material_name_snapshot VARCHAR(120) NOT NULL,
    unit_price_snapshot NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_service_order_material_lines_order
        FOREIGN KEY (service_order_id) REFERENCES service_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_order_material_lines_inventory_item
        FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id),
    CONSTRAINT ck_service_order_material_lines_price CHECK (unit_price_snapshot >= 0),
    CONSTRAINT ck_service_order_material_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_service_order_material_lines_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE INDEX ix_service_order_material_lines_order
    ON service_order_material_lines (service_order_id);

CREATE TABLE service_order_estimates (
    id UUID PRIMARY KEY,
    service_order_id UUID NOT NULL,
    status VARCHAR(10) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_service_order_estimates_order
        FOREIGN KEY (service_order_id) REFERENCES service_orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_service_order_estimates_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_service_order_estimates_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_service_order_estimates_response
        CHECK ((status = 'PENDING') = (responded_at IS NULL)),
    CONSTRAINT ck_service_order_estimates_deletion_audit
        CHECK ((deleted_at IS NULL) = (deleted_by IS NULL))
);

CREATE INDEX ix_service_order_estimates_order
    ON service_order_estimates (service_order_id);

CREATE TABLE service_order_estimate_decisions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    service_order_id UUID NOT NULL,
    estimate_id UUID NOT NULL,
    decision VARCHAR(10) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_service_order_estimate_decisions_order
        FOREIGN KEY (service_order_id) REFERENCES service_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_order_estimate_decisions_estimate
        FOREIGN KEY (estimate_id) REFERENCES service_order_estimates (id) ON DELETE CASCADE,
    CONSTRAINT ck_service_order_estimate_decisions_decision CHECK (decision IN ('APPROVE', 'REJECT')),
    CONSTRAINT uk_service_order_estimate_decisions_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_service_order_estimate_decisions_order
    ON service_order_estimate_decisions (service_order_id);

CREATE TABLE service_order_status_history (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    service_order_id UUID NOT NULL,
    previous_status VARCHAR(25),
    status VARCHAR(25) NOT NULL,
    actor VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_service_order_status_history_order
        FOREIGN KEY (service_order_id) REFERENCES service_orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_service_order_status_history_previous_status CHECK (
        previous_status IS NULL OR previous_status IN (
            'RECEIVED', 'UNDER_DIAGNOSIS', 'AWAITING_APPROVAL',
            'IN_PROGRESS', 'COMPLETED', 'DELIVERED'
        )
    ),
    CONSTRAINT ck_service_order_status_history_status CHECK (status IN (
        'RECEIVED', 'UNDER_DIAGNOSIS', 'AWAITING_APPROVAL',
        'IN_PROGRESS', 'COMPLETED', 'DELIVERED'
    ))
);

CREATE INDEX ix_service_order_status_history_order_time
    ON service_order_status_history (service_order_id, occurred_at);

ALTER TABLE inventory_reservations
    ADD CONSTRAINT fk_inventory_reservations_service_order
    FOREIGN KEY (service_order_id) REFERENCES service_orders (id);
