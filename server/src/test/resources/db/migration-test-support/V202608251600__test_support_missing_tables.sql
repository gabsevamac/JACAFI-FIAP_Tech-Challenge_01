-- REMENDO DE TESTE — NAO E MIGRACAO DE PRODUCAO. APAGAR QUANDO O DEFEITO ABAIXO FOR CORRIGIDO.
--
-- Problema: as entidades da fatia service_order (ServiceOrder, Service, ServiceOrderService) nao
-- possuem migracao Flyway. Com ddl-auto=validate, o Hibernate compara o mapeamento com o schema
-- e falha na subida do contexto:
--
--     SchemaManagementException: Schema validation: missing table [service_orders]
--
-- Como a validacao acontece na criacao do EntityManagerFactory, QUALQUER @SpringBootTest quebra,
-- inclusive os testes desta fatia, que nada tem a ver com ordem de servico.
--
-- Este arquivo existe apenas no classpath de TESTE (spring.flyway.locations no perfil test) para
-- destravar os testes de integracao da fatia vehicle. Ele espelha as entidades como estao HOJE e
-- por isso reproduz duas divergencias em relacao ao §9 do dicionario, deliberadamente:
--
--   * client_id  deveria ser customer_id  (Cliente -> Customer)
--   * o enum de status usa IN_DIAGNOSIS / PENDING_APPROVAL, e nao UNDER_DIAGNOSIS /
--     AWAITING_APPROVAL
--
-- O dono da fatia escreve a migracao definitiva, com os nomes corretos. Este remendo sai no mesmo
-- commit. Enquanto ele existir, a aplicacao continua NAO subindo em producao, porque lá estas
-- tabelas seguem inexistentes — o remendo destrava o build, nao o deploy.

CREATE TABLE services (
    id          UUID          PRIMARY KEY,
    description VARCHAR(45)   NOT NULL,
    base_price  NUMERIC(38,2) NOT NULL
);

CREATE TABLE service_orders (
    id         UUID          PRIMARY KEY,
    status     VARCHAR(25)   NOT NULL,
    total      NUMERIC(38,2) NOT NULL,
    vehicle_id UUID          NOT NULL,
    client_id  UUID          NOT NULL,
    created_at TIMESTAMP(6)  NOT NULL,
    updated_at TIMESTAMP(6)  NOT NULL
);

CREATE TABLE service_orders_service (
    service_order_id UUID          NOT NULL,
    service_id       UUID          NOT NULL,
    price_at_sale    NUMERIC(38,2) NOT NULL,
    quantity         INTEGER       NOT NULL,
    PRIMARY KEY (service_order_id, service_id),
    CONSTRAINT fk_sos_service_order FOREIGN KEY (service_order_id) REFERENCES service_orders (id),
    CONSTRAINT fk_sos_service       FOREIGN KEY (service_id)       REFERENCES services (id)
);
