-- Auditoria, em duas metades que costumam ser confundidas.
--
-- (a) Auditoria tecnica: colunas em cada tabela de negocio dizendo quando a linha foi criada e
--     alterada, por quem, e se foi removida logicamente. Responde "o estado atual desta linha
--     veio de onde".
--
-- (b) Trilha de auditoria: tabela append-only registrando cada alteracao de campo, com o valor
--     antes e depois. Responde "qual era o valor em tal data, e quem o mudou".
--
-- A primeira nao substitui a segunda: updated_by guarda apenas o ultimo autor, e sobrescreve o
-- anterior a cada gravacao.

-- ---------------------------------------------------------------------------------------------
-- (a) Auditoria tecnica
-- ---------------------------------------------------------------------------------------------
--
-- created_at / updated_at ja existem nas tres tabelas. O que falta e a autoria, a remocao logica
-- e a versao.
--
-- version sustenta bloqueio otimista. Sem ela, duas telas abertas sobre a mesma ordem de servico
-- gravam por cima uma da outra e a ultima vence em silencio — que num fluxo de aprovacao de
-- orcamento significa aprovar um valor que o cliente nao viu.

-- vehicles ja nasce com as colunas de auditoria na V202608251500.

ALTER TABLE customers
    ADD COLUMN created_by VARCHAR(120),
    ADD COLUMN updated_by VARCHAR(120),
    ADD COLUMN deleted_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN deleted_by VARCHAR(120),
    ADD COLUMN version    BIGINT;

ALTER TABLE service_orders
    ADD COLUMN created_by VARCHAR(120),
    ADD COLUMN updated_by VARCHAR(120),
    ADD COLUMN deleted_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN deleted_by VARCHAR(120),
    ADD COLUMN version    BIGINT;

-- Linhas que ja existem nao tem autor conhecido, e inventar um seria pior do que admitir isso.
-- "system" e o mesmo valor que o AuditorAware usa quando nao ha autenticacao — migracao, job,
-- seed. Depois do backfill as colunas ficam NOT NULL, para que a ausencia de autor deixe de ser
-- um estado representavel.
UPDATE customers      SET created_by = 'system', updated_by = 'system', version = 0;
UPDATE service_orders SET created_by = 'system', updated_by = 'system', version = 0;

ALTER TABLE customers
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL,
    ALTER COLUMN version    SET NOT NULL;

ALTER TABLE service_orders
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL,
    ALTER COLUMN version    SET NOT NULL;

-- ---------------------------------------------------------------------------------------------
-- (b) Trilha de auditoria
-- ---------------------------------------------------------------------------------------------
--
-- ATENCAO — ESTA TABELA ARMAZENA DADOS PESSOAIS (LGPD Art. 5 I).
--
-- old_value e new_value guardam o valor integro do campo alterado, e entre os campos auditados
-- estao a placa do veiculo e o CPF/CNPJ do cliente. Os valores NAO sao mascarados aqui: uma
-- trilha que registrasse "a placa mudou de *** para ***" nao responderia a unica pergunta que
-- motiva sua existencia, que e qual era o valor antes.
--
-- Base legal da retencao: Art. 16, I — conservacao para cumprimento de obrigacao legal ou
-- regulatoria. O direito a eliminacao do Art. 18, VI se ressalva expressamente as hipoteses do
-- Art. 16, entao a trilha sobrevive a remocao do veiculo.
--
-- Consequencia assumida, e nao defeito: a remocao de veiculo deixa de eliminar a placa do
-- sistema. Ela continua apagando vehicles.license_plate, substituindo-a por um token irreversivel
-- que libera o indice unico parcial para recadastro — mas o valor anterior permanece aqui. O
-- historico do valor da placa e requisito de negocio (hot spots HS7 a HS10, sobre mutabilidade de
-- placa) e nao existe sem guardar o valor.
--
-- CONSEQUENCIAS OPERACIONAIS, que precisam constar da politica de privacidade:
--   - a tabela entra na politica de retencao e precisa de prazo definido;
--   - um pedido de titular (Art. 18) alcanca estas linhas e precisa de procedimento;
--   - os valores NUNCA podem ser logados nem devolvidos por API sem passar pelo Masker.

CREATE TABLE audit_trail (
    id             BIGSERIAL    PRIMARY KEY,
    aggregate_type VARCHAR(60)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    field_name     VARCHAR(60)  NOT NULL,
    -- Nulo e significativo: campo que nao tinha valor antes, ou que passou a nao ter.
    old_value      TEXT,
    new_value      TEXT,
    -- Nulo por decisao, nao por omissao. HS9 registra que a semantica do motivo ainda e ambigua
    -- — corrigir um erro de digitacao e trocar a placa apos emplacamento sao coisas diferentes, e
    -- o grupo ainda nao decidiu se o motivo e texto livre ou lista fechada. Exigir preenchimento
    -- agora produziria um campo cheio de "atualizacao".
    reason         VARCHAR(200),
    changed_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    changed_by     VARCHAR(120) NOT NULL
);

-- A consulta que a trilha existe para responder e "historico deste agregado, em ordem".
CREATE INDEX ix_audit_trail_aggregate
    ON audit_trail (aggregate_type, aggregate_id, changed_at);

-- Sem chave estrangeira para nenhuma tabela de negocio, de proposito: a trilha precisa sobreviver
-- a qualquer expurgo futuro do dado auditado. Auditoria que some junto com o dado nao e prova.
--
-- Append-only e uma regra de aplicacao, nao uma restricao do schema. Garantir no banco exigiria
-- revogar UPDATE e DELETE do usuario da aplicacao, o que nao da para fazer aqui sem quebrar o
-- Flyway, que usa a mesma conexao. Fica registrado como endurecimento para producao.
