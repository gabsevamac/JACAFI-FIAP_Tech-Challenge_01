-- Colapso de `parties` dentro de `customers`, e substituicao do enum person_type pelo tipo do
-- proprio identificador.
--
-- O modelo anterior separava a pessoa juridica (Party) do papel de cliente (Customer) — o padrao
-- Party do Fowler, que se paga quando a mesma pessoa tambem e fornecedor ou funcionario. O §2
-- coloca compras, fornecedores e folha de pagamento FORA deste contexto delimitado, entao nao
-- existe nem esta previsto um segundo papel: era uma abstracao com uma implementacao, mais um
-- join.

ALTER TABLE customers ADD COLUMN tax_id     VARCHAR(14);
ALTER TABLE customers ADD COLUMN name       VARCHAR(150);
ALTER TABLE customers ADD COLUMN trade_name VARCHAR(150);

-- Migracao de dados antes de apertar as restricoes. O banco e descartavel hoje, mas uma migracao
-- que perde linha ensina o habito errado para quando ele deixar de ser.
UPDATE customers c
   SET tax_id     = p.tax_id,
       name       = p.name,
       trade_name = p.trade_name
  FROM parties p
 WHERE p.id = c.party_id;

ALTER TABLE customers ALTER COLUMN tax_id SET NOT NULL;
ALTER TABLE customers ALTER COLUMN name   SET NOT NULL;

ALTER TABLE customers DROP COLUMN party_id;
DROP TABLE parties;

-- Sem coluna discriminadora: o valor discrimina. CPF normalizado tem 11 caracteres e CNPJ tem 14,
-- faixas que nao se sobrepoem, entao a unicidade em tax_id sozinho e equivalente ao par
-- (person_type, tax_id) de antes — e nao ha dado derivado que possa divergir do valor.
ALTER TABLE customers ADD CONSTRAINT uk_customers_tax_id UNIQUE (tax_id);

ALTER TABLE customers ADD CONSTRAINT ck_customers_tax_id_format CHECK (
    tax_id ~ '^[0-9]{11}$' OR tax_id ~ '^[0-9A-Z]{12}[0-9]{2}$'
);

-- Nome fantasia so existe para pessoa juridica, e pessoa juridica e o que tem 14 caracteres.
ALTER TABLE customers ADD CONSTRAINT ck_customers_trade_name CHECK (
    length(tax_id) = 14 OR trade_name IS NULL
);
