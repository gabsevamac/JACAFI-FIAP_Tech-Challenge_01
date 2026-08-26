CREATE TABLE inventory_items (
    id               UUID          PRIMARY KEY,
    name             VARCHAR(120)  NOT NULL,
    type             VARCHAR(10)   NOT NULL,
    unit_price       NUMERIC(12,2) NOT NULL,
    stock_on_hand    INTEGER       NOT NULL,
    registered_at    TIMESTAMP(6)  WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP(6)  WITH TIME ZONE NOT NULL,
    removed_at       TIMESTAMP(6)  WITH TIME ZONE,
    CONSTRAINT ck_inventory_items_type CHECK (type IN ('PART', 'SUPPLY')),
    CONSTRAINT ck_inventory_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_inventory_items_stock CHECK (stock_on_hand >= 0)
);

-- Unicidade de nome ENTRE ITENS ATIVOS, nao na tabela inteira.
--
-- Duas linhas para a mesma peca sao dois saldos para uma prateleira so, e a oficina deixa de
-- conseguir responder quantas tem — que e exatamente a dor "falhas no controle de pecas"
-- reaparecendo dentro do sistema feito para acaba-la.
--
-- UPPER(name) porque a comparacao na aplicacao ignora caixa (existsByNameIgnoreCase...): sem a
-- mesma normalizacao aqui, o indice deixaria passar o que a regra de negocio recusa.
--
-- O indice parcial e o que permite as duas coisas ao mesmo tempo: o nome de um item removido nao
-- bloqueia um novo cadastro, e a linha antiga continua existindo para as baixas que a referenciam.
CREATE UNIQUE INDEX ux_inventory_items_name_active
    ON inventory_items (UPPER(name))
    WHERE removed_at IS NULL;

-- Suporta a listagem paginada por tipo, que tambem so ve itens ativos.
CREATE INDEX ix_inventory_items_type_active
    ON inventory_items (type, name, id)
    WHERE removed_at IS NULL;

-- Reservas EM ABERTO. Uma reserva termina sendo liberada ou baixada, e o que aconteceu com ela
-- fica na trilha, que e append-only e serve de razao. Guardar reservas encerradas aqui tambem
-- faria o saldo depender de qual das duas fontes a consulta resolveu ler.
CREATE TABLE inventory_reservations (
    id                UUID         PRIMARY KEY,
    inventory_item_id UUID         NOT NULL REFERENCES inventory_items (id),
    service_order_id  UUID         NOT NULL,
    quantity          INTEGER      NOT NULL,
    reserved_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inventory_reservations_quantity CHECK (quantity > 0)
);

-- Uma ordem tem no maximo uma reserva aberta por item: reparo adicional aumenta a reserva que ja
-- existe, nao abre uma segunda. Com duas linhas, liberar e baixar deixariam de saber qual delas
-- encerrar.
CREATE UNIQUE INDEX ux_inventory_reservations_item_order
    ON inventory_reservations (inventory_item_id, service_order_id);

CREATE INDEX ix_inventory_reservations_service_order
    ON inventory_reservations (service_order_id);

CREATE TABLE inventory_audit_entries (
    id                BIGSERIAL    PRIMARY KEY,
    inventory_item_id UUID         NOT NULL,
    operation         VARCHAR(20)  NOT NULL,
    service_order_id  UUID,
    quantity          INTEGER,
    actor             VARCHAR(120) NOT NULL,
    occurred_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inventory_audit_entries_operation CHECK (
        operation IN ('REGISTERED', 'UPDATED', 'REMOVED', 'REPLENISHED',
                      'RESERVED', 'RELEASED', 'WITHDRAWN')
    ),

    CONSTRAINT ck_inventory_audit_entries_movement CHECK (
        (operation IN ('RESERVED', 'RELEASED', 'WITHDRAWN')
             AND service_order_id IS NOT NULL AND quantity IS NOT NULL)
        OR (operation = 'REPLENISHED'
             AND service_order_id IS NULL AND quantity IS NOT NULL)
        OR (operation IN ('REGISTERED', 'UPDATED', 'REMOVED')
             AND service_order_id IS NULL AND quantity IS NULL)
    )
);

CREATE INDEX ix_inventory_audit_entries_item ON inventory_audit_entries (inventory_item_id, occurred_at);
CREATE INDEX ix_inventory_audit_entries_service_order ON inventory_audit_entries (service_order_id, occurred_at);
