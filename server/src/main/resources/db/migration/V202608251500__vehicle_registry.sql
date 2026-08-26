-- Fatia vehicle: cadastro de veiculos e trilha de auditoria.
--
-- Sem chave estrangeira para cliente. A fatia customer e de outro integrante e tem ciclo de vida
-- proprio; o vinculo e por identificador, conforme a regra de fronteira do README.

CREATE TABLE vehicles (
    id            UUID         PRIMARY KEY,
    -- Guarda a placa enquanto o veiculo esta ativo e um token irreversivel depois da remocao,
    -- por isso e mais larga que os sete caracteres de uma placa.
    license_plate VARCHAR(64)  NOT NULL,
    make          VARCHAR(60)  NOT NULL,
    model         VARCHAR(60)  NOT NULL,
    model_year    INTEGER      NOT NULL,
    customer_id   UUID         NOT NULL,

    -- Colunas de auditoria tecnica, iguais em toda tabela de negocio (AuditableEntity).
    -- Os nomes sao os da superclasse, e nao os do dicionario de veiculo: created_at em vez de
    -- registered_at, deleted_at em vez de removed_at. O vocabulario de negocio continua no
    -- dominio — Vehicle.getRegisteredAt(), o evento VehicleRemoved — e o mapper faz a ponte. Um
    -- schema onde cada fatia nomeia auditoria a seu modo obriga qualquer consulta transversal a
    -- decorar quatro vocabularios.
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    -- Preenchido na remocao logica. A presenca deste campo e o que tira a linha de todas as
    -- consultas, e e o predicado do indice unico parcial abaixo.
    deleted_at TIMESTAMP(6) WITH TIME ZONE,
    deleted_by VARCHAR(120),
    version    BIGINT       NOT NULL
);

-- Unicidade de placa ENTRE VEICULOS ATIVOS, nao na tabela inteira.
--
-- O indice parcial e o que permite as duas coisas ao mesmo tempo: a placa de um veiculo removido
-- nao bloqueia um novo cadastro, e a linha antiga continua existindo para o historico de servico
-- exigido por obrigacao legal e por garantia (LGPD Art. 16 I).
--
-- E tambem o motivo de o teste de integracao exigir Postgres de verdade: H2 nao implementa
-- indice unico com predicado, entao um teste verde nele nao provaria nada sobre esta restricao.
CREATE UNIQUE INDEX ux_vehicles_license_plate_active
    ON vehicles (license_plate)
    WHERE deleted_at IS NULL;

-- Suporta a listagem paginada por cliente, que tambem so ve veiculos ativos.
CREATE INDEX ix_vehicles_customer_active
    ON vehicles (customer_id, created_at, id)
    WHERE deleted_at IS NULL;

-- Trilha de auditoria (LGPD Art. 37): quem, quando e qual operacao.
--
-- Registra QUE operacao ocorreu, sem os valores dos campos. O historico do valor em si vive na
-- tabela audit_trail, compartilhada, que guarda old_value e new_value integros — inclusive a
-- placa, retida sob o Art. 16 I e portanto sobrevivendo a remocao do veiculo.
--
-- As duas coexistem porque respondem perguntas diferentes: esta responde "quem mexeu neste
-- veiculo e quando", audit_trail responde "qual era o valor deste campo em tal data".
CREATE TABLE vehicle_audit_entries (
    id          BIGSERIAL    PRIMARY KEY,
    vehicle_id  UUID         NOT NULL,
    operation   VARCHAR(20)  NOT NULL,
    actor       VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_vehicle_audit_entries_vehicle ON vehicle_audit_entries (vehicle_id, occurred_at);

-- Sem FK de vehicle_audit_entries para vehicles de proposito: a trilha precisa sobreviver a
-- qualquer expurgo futuro da tabela de veiculos. Auditoria que desaparece junto com o dado
-- auditado nao serve de prova.
