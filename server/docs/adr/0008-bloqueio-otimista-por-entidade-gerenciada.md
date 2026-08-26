# 0008 — Bloqueio otimista por cópia para entidade gerenciada

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

`@Version` entrou em [0006](0006-auditoria-tecnica-com-vocabulario-padronizado.md) para que
duas telas abertas sobre a mesma ordem de serviço não gravem uma por cima da outra. Num
fluxo de aprovação de orçamento, a última escrita vencendo em silêncio significa registrar
aprovação de um total que o cliente não viu — a invariante central do sistema derrotada por
uma corrida, não por um defeito na regra.

O adaptador de veículo reconstruía uma entidade **destacada** a partir do agregado a cada
gravação. Uma instância reconstruída carrega `version = 0` sempre. Resultado: a segunda
gravação de uma linha colidia com a **primeira dela mesma**, com
`StaleObjectStateException`. O bloqueio otimista, do jeito que estava, quebrava toda
segunda escrita.

## Decisão

O adaptador **carrega a linha e copia estado para dentro dela**, em vez de mesclar uma
instância destacada:

```java
jpaRepository.findById(vehicle.getId())
    .ifPresentOrElse(
        managed -> mapper.copyInto(managed, vehicle),
        () -> jpaRepository.save(mapper.toEntity(vehicle)));
```

Isso também dá ao bloqueio o significado correto: o Hibernate compara a versão contra o
banco no *flush*, então o que conflita é a escrita de **outra transação**, e não a anterior
desta mesma.

`VehicleJpaEntity.applyState(...)` move todo o estado mutável numa chamada, o que mantém
"linha parcialmente atualizada" fora dos estados representáveis — a razão pela qual a
entidade não tem *setters*.

## Alternativas consideradas

**`version` no agregado.** É o padrão que Vernon descreve, e funcionaria: a versão viajaria
de volta com a escrita. Recusado por fazer o domínio carregar uma preocupação de
persistência — o agregado passaria a ter um campo cujo único significado é como o ORM
detecta concorrência.

**Ler a versão atual antes de mesclar.** Faria o build passar e **desligaria** o bloqueio:
usando sempre a versão mais recente, escritas concorrentes nunca conflitariam. Pior que não
ter `@Version`, porque pareceria protegido.

**Não usar `@Version` em `vehicles`.** Deixaria a fatia fora da proteção que a superclasse
promete para todas.

## Consequências

- `VehicleRepositoryAdapter.save` passou a ser `@Transactional`, e isso é obrigatório:
  fora de transação, `findById` devolve entidade **destacada** e a mutação não grava nada,
  **em silêncio**. Esse segundo defeito estava escondido atrás do primeiro e só apareceu
  porque o teste do repositório chama o adaptador sem transação.
- Custa um `SELECT` antes de cada `UPDATE`. Em contrapartida, o `merge` anterior já fazia
  esse mesmo select.
- Guardado por `AuditingIT.advancesTheVersion`.
