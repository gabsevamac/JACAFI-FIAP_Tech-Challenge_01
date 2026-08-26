# 0007 — Remoção lógica por filtro explícito

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Um veículo removido mantém a linha: o histórico de serviço é obrigação legal e de garantia
(LGPD Art. 16, I). Mas ele não responde a consulta nenhuma, e sua placa não bloqueia um
recadastro — o que o índice único parcial (`WHERE deleted_at IS NULL`) sustenta.

Duas formas de garantir que a linha removida não apareça, e a escolha precisava ser feita
uma vez para todas as fatias.

O comportamento do `@SQLRestriction` foi **medido** no Hibernate 7 / Boot 4.1.0, não
presumido:

| Caminho de leitura | Filtra a linha removida? |
|---|---|
| `em.find()` por chave primária | **sim** |
| JPQL, Criteria, *derived query* | **sim** |
| **Query nativa** | **não** |

O `em.find()` respeitar a restrição contraria o folclore herdado do antigo `@Where`.

## Decisão

**Filtro explícito** nos métodos de repositório — `findByIdAndDeletedAtIsNull`,
`existsByLicensePlateAndDeletedAtIsNull`.

O critério **não foi capacidade**: nenhuma das duas opções filtra query nativa. Foi a
**crença que cada uma cria**. Com `@SQLRestriction`, a remoção lógica parece resolvida
globalmente, então uma query nativa devolvendo linha removida é uma surpresa. Com filtro
explícito não existe essa expectativa.

Some-se que o relatório de análise de vulnerabilidades é entregável do desafio, e
"listagem devolveu registro removido" é achado clássico de scanner. A opção que falha de
forma **visível** é preferível à que falha em silêncio.

## Alternativas consideradas

**`@SQLDelete` + `@SQLRestriction`.** Vantagem real: impossível esquecer numa consulta
JPQL. Duas desvantagens: a regra some do código e passa a viver numa anotação de classe, e
um `@ManyToOne` para uma linha removida devolve `null` sem aviso mesmo com FK `NOT NULL`.
Hoje não há associação ORM para veículo, então esse risco é teórico — mas seria o primeiro
a aparecer quando houver.

**Filtro por `@Filter` do Hibernate.** Exige ativação por sessão, o que reintroduz o
esquecimento sem eliminar o problema da query nativa.

## Consequências

- Cada consulta nova precisa repetir o predicado. A fatia `vehicle` tem quatro métodos com
  o sufixo; se virarem quinze, a chance de esquecer um cresce e vale reavaliar.
- **Regra que fica registrada:** query nativa nesta base sempre repete o predicado
  `deleted_at IS NULL`. Nenhum mecanismo a impõe.
- O contra-argumento é legítimo. Se o grupo preferir a garantia sobre a visibilidade,
  `@SQLRestriction` é defensável e este ADR deve ser substituído, não editado.
