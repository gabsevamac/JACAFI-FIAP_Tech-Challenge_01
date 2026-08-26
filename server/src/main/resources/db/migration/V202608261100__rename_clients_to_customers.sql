-- Alinhamento com a linguagem ubiqua: §9 do dicionario fixa Cliente -> Customer.
--
-- Migracao nova, e nao edicao da V20260824_01, porque aquela ja esta commitada na main e pode
-- ter sido aplicada nas maquinas dos outros integrantes — alterar o arquivo mudaria o checksum
-- e faria o Flyway falhar na subida deles.

ALTER TABLE clients RENAME TO customers;

-- O Postgres mantem os nomes automaticos vindos da tabela antiga. Renomeados para nao sobrar
-- "clients" no schema depois de o conceito ter deixado de existir.
ALTER TABLE customers RENAME CONSTRAINT clients_pkey TO customers_pkey;
ALTER TABLE customers RENAME CONSTRAINT clients_party_id_key TO customers_party_id_key;

-- A coluna de vinculo em service_orders acompanha, junto com o campo ServiceOrder.customerId.
-- A chave estrangeira segue o rename da tabela sozinha; o nome dela e que precisa ser dito.
ALTER TABLE service_orders RENAME COLUMN client_id TO customer_id;
ALTER TABLE service_orders RENAME CONSTRAINT service_orders_client_id_fkey
    TO service_orders_customer_id_fkey;

-- Depois disto, "client" nao aparece mais em nenhum lugar do schema.
