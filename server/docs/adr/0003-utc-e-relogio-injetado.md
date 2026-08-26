# 0003 — UTC em todas as camadas e relógio injetado

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Um dos requisitos do desafio é monitorar o **tempo médio de execução dos serviços**. Isso é
subtração entre timestamps, e subtração só é confiável se todas as camadas concordarem
sobre o que "agora" significa.

O risco não é teórico. A mesma ordem de serviço aberta por um container em UTC e por um
desenvolvedor rodando pela IDE em `America/Sao_Paulo` gravava valores com três horas de
diferença — sem erro, sem aviso, e sem nada no schema que permitisse descobrir depois qual
foi qual. A conta continua devolvendo um número plausível.

Além disso, `@CreationTimestamp` e `Instant.now()` leem o relógio do sistema, o que torna
qualquer asserção sobre tempo uma tolerância em vez de uma igualdade — e uma tolerância
passa na máquina rápida e falha na CI carregada.

## Decisão

**UTC fixado nas quatro camadas:**

| Camada | Onde | O quê |
|---|---|---|
| Container da aplicação | `Dockerfile` | `ENV TZ=UTC` |
| Container do banco | `docker-compose.yaml` | `TZ` (SO) e `PGTZ` (sessão libpq) |
| JDBC / Hibernate | `application.yaml` | `hibernate.jdbc.time_zone=UTC` |
| JVM | `ApplicationTimeZone.enforceUtc()` no `main` | cobre quem roda fora do Docker |

**Toda coluna de tempo é `TIMESTAMP WITH TIME ZONE`**, verificado por
`DatabaseMigrationTest`, que consulta o `information_schema` e reprova qualquer tabela
nossa com coluna sem fuso — a verificação é do schema inteiro, então uma fatia futura é
barrada sem depender de alguém lembrar da regra na revisão.

**O relógio é injetado, nunca lido.** Nenhuma classe chama `Instant.now()`; todas recebem o
bean `Clock`. A auditoria JPA é alimentada por `ClockDateTimeProvider`, e não pelo relógio
do sistema.

## Alternativas consideradas

**Só `TZ=UTC` no container.** Cobre o deploy e deixa de fora o caso onde o defeito melhor
se esconde: a máquina de quem desenvolve, onde ele só aparece para outra pessoa.

**`LocalDateTime` com convenção "sempre UTC".** É o que existia em `customers` e
`service_orders`, e é o que produziu o defeito: a coluna não registra qual fuso foi usado,
então a convenção não é verificável.

**Relógio do sistema com tolerância nos testes.** Torna a suíte não determinística no
exato ponto que o requisito de tempo médio depende.

## Consequências

- `LocalDateTime` deixou de existir no código; `java.util.Date` e `java.sql.Timestamp` são
  barrados por [0002](0002-checkstyle-restrito-a-imports.md).
- `FixedClockConfiguration` permite que testes de integração afirmem **igualdade** sobre
  `createdAt`, não aproximação.
- Mudança de contrato observável: os endpoints de `customers` passaram a devolver
  `"2026-01-15T10:30:00.123456Z"` em vez de `"2026-01-15T10:30:00.123456"`. É o formato que
  `vehicle` já devolvia, mas quebra cliente com parsing estrito.
- **Registrado por honestidade:** `hibernate.jdbc.time_zone=UTC` foi medido e **não é** o
  que protege o round-trip de `Instant` contra `TIMESTAMPTZ` — os dois são absolutos e não
  há conversão para errar. A propriedade permanece como defesa em profundidade, para o dia
  em que alguém mapear um tipo que dependa dela.
