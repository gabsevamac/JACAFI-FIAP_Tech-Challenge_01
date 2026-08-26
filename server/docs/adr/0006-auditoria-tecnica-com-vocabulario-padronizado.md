# 0006 — Auditoria técnica com vocabulário padronizado

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Toda linha persistida precisa dizer quando foi criada e alterada, por quem, e se foi
removida logicamente. Isso responde *"o estado atual desta linha veio de onde"* — pergunta
diferente da que a trilha de auditoria responde, porque `updated_by` guarda só o último
autor e sobrescreve o anterior a cada gravação.

Havia um conflito de vocabulário. A fatia `vehicle` chamava as colunas de `registered_at` e
`removed_at`, termos do §9 do dicionário (`VehicleRegistered`, `VehicleRemoved`), e o
segundo sustenta o índice único parcial da placa.

Restrição de arquitetura: classes de domínio não recebem anotação de JPA.

## Decisão

`shared/persistence/AuditableEntity`, `@MappedSuperclass` com
`@EntityListeners(AuditingEntityListener.class)`, herdada por `vehicles`, `customers` e
`service_orders`: `createdAt/By`, `updatedAt/By`, `deletedAt/By` e `version`.

**Os nomes das colunas são idênticos em toda tabela.** `vehicles` renomeou `registered_at`
para `created_at` e `removed_at` para `deleted_at`. O vocabulário de negócio continua vivo
no domínio — `Vehicle.getRegisteredAt()`, o evento `VehicleRemoved` — e a ponte fica no
mapper de persistência, único lugar obrigado a conhecer os dois.

O autor vem de `JwtAuditorAware`, que lê o subject do JWT. Sem autenticação — migração,
job, seed — grava `"system"`, **nunca `null`**.

## Alternativas consideradas

**`@AttributeOverride` por fatia**, preservando `registered_at` e `removed_at`. Foi a
primeira tentativa. Recusada: um schema onde cada fatia nomeia auditoria a seu modo obriga
qualquer consulta transversal a decorar quatro vocabulários, e o ganho é consistência de
nome, não de conceito. Padronizar a linguagem é o propósito da superclasse.

**Coluna de autor nulável.** Recusada: obriga todo relatório de autoria a decidir o que
`null` significa, e a decisão usual é omitir a linha — o que transforma uma escrita sem
autor numa escrita invisível. `"system"` é uma afirmação verificável; `null` é uma ausência.

**Hibernate Envers.** Vetado pelo enunciado da tarefa: acopla o domínio e é pesado demais
para o MVP.

**`@CreationTimestamp` do Hibernate.** Lê o relógio do sistema, não o bean `Clock` — ver
[0003](0003-utc-e-relogio-injetado.md).

## Consequências

- As *derived queries* do Spring Data passaram a dizer `AndDeletedAtIsNull`, porque a
  consulta é derivada do nome da **propriedade**, herdada da superclasse — não do nome da
  coluna.
- Consultas transversais a fatias passam a ser possíveis sem tradução.
- `AuditTrailJpaEntity` deliberadamente **não** herda: uma entrada de trilha é escrita uma
  vez e nunca alterada, e herdar `updatedBy`, `deletedAt` e `version` lhe daria exatamente
  as operações que ela existe para tornar impossíveis.
- O grupo precisa saber do rename: qualquer query nativa contra `vehicles` que use
  `registered_at` ou `removed_at` quebra.
