CREATE TABLE parties (
    id UUID PRIMARY KEY,
    person_type VARCHAR(20) NOT NULL,
    tax_id VARCHAR(14) NOT NULL,
    name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150),
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_parties_person_type CHECK (person_type IN ('INDIVIDUAL', 'LEGAL_ENTITY')),
    CONSTRAINT ck_parties_tax_identifier_format CHECK (
        (person_type = 'INDIVIDUAL' AND tax_id ~ '^[0-9]{11}$') OR
        (person_type = 'LEGAL_ENTITY' AND tax_id ~ '^[0-9A-Z]{12}[0-9]{2}$')
    ),
    CONSTRAINT ck_parties_trade_name CHECK (person_type = 'LEGAL_ENTITY' OR trade_name IS NULL)
);

CREATE UNIQUE INDEX uk_parties_tax_identifier ON parties (person_type, tax_id);

CREATE TABLE clients (
    id UUID PRIMARY KEY,
    party_id UUID NOT NULL UNIQUE REFERENCES parties (id),
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE services (
    id UUID PRIMARY KEY,
    description VARCHAR(45) NOT NULL,
    base_price NUMERIC(38, 2) NOT NULL
);

CREATE TABLE service_orders (
    id UUID PRIMARY KEY,
    status VARCHAR(25) NOT NULL,
    total NUMERIC(38, 2) NOT NULL,
    vehicle_id UUID NOT NULL,
    client_id UUID NOT NULL REFERENCES clients (id),
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE service_orders_service (
    service_order_id UUID NOT NULL REFERENCES service_orders (id),
    service_id UUID NOT NULL REFERENCES services (id),
    price_at_sale NUMERIC(38, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    PRIMARY KEY (service_order_id, service_id),
    CONSTRAINT ck_service_order_service_quantity CHECK (quantity > 0),
    CONSTRAINT ck_service_order_service_price CHECK (price_at_sale >= 0)
);
