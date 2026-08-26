-- Converte as colunas de tempo de customers e service_orders para TIMESTAMP WITH TIME ZONE.
--
-- Estado anterior: TIMESTAMP(6) WITHOUT TIME ZONE, mapeado por LocalDateTime com
-- @CreationTimestamp / @UpdateTimestamp. Nessa combinacao o Hibernate deriva o valor do fuso
-- default da JVM, e a coluna nao guarda qual era esse fuso. O resultado e uma hora de parede sem
-- procedencia: a mesma ordem de servico aberta por um container em UTC e por um desenvolvedor
-- rodando pela IDE em America/Sao_Paulo grava valores com tres horas de diferenca, sem erro e
-- sem nada no schema que permita descobrir depois qual foi qual.
--
-- Isso importa aqui mais do que importaria em outro projeto: um dos requisitos do desafio e
-- monitorar o tempo medio de execucao dos servicos, que e subtracao entre created_at e o
-- instante de entrega. Uma subtracao entre dois valores de fusos diferentes devolve um numero
-- plausivel e errado.
--
-- A fatia vehicle ja usava TIMESTAMP WITH TIME ZONE com Instant desde a V202608251500. Esta
-- migracao alinha as outras duas tabelas ao mesmo contrato, que e o que o CLAUDE.md ja exigia.
--
-- Sobre a conversao dos dados existentes: AT TIME ZONE 'UTC' interpreta o valor gravado como
-- tendo sido escrito em UTC. Para linhas gravadas por um ambiente em UTC — o container, e agora
-- tambem a JVM local, que passou a fixar o default — a conversao e exata. Linhas que tenham sido
-- gravadas por uma JVM em outro fuso antes desta serie de mudancas ficam deslocadas, e nao ha
-- como corrigi-las: a informacao necessaria nunca chegou a ser gravada. Em desenvolvimento o
-- banco sobe do zero, entao o caso nao se apresenta; o registro fica para quem for promover isto
-- a um ambiente com dado real.

ALTER TABLE customers
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE service_orders
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING updated_at AT TIME ZONE 'UTC';
