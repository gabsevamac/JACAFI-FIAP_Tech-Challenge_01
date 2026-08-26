# 0004 — Precisão de tempo em microssegundos

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Medido no endpoint de veículos: o **mesmo campo do mesmo recurso** voltava de duas formas
diferentes.

```
POST  →  "registeredAt":"2026-08-26T14:20:48.492948227Z"
GET   →  "registeredAt":"2026-08-26T14:20:48.492948Z"
```

`Clock.systemUTC()` resolve em nanossegundos. O `TIMESTAMPTZ` do Postgres guarda
microssegundos. Os três dígitos excedentes sumiam na escrita — sem erro, sem aviso.

Um cliente que guardasse a resposta da criação e a comparasse com uma leitura posterior
veria divergência num recurso que ninguém alterou. É o tipo de defeito que vira chamado de
suporte irreprodutível.

## Decisão

O bean `Clock` passa a truncar na precisão que o banco consegue guardar:

```java
Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))
```

Arredondar **na origem** elimina a discrepância em vez de documentá-la: a aplicação passa a
produzir exatamente o que o armazenamento comporta. A precisão perdida é precisão que nunca
foi persistida.

## Alternativas consideradas

**Documentar e conviver.** Transfere para cada cliente a obrigação de saber que a
comparação precisa ser tolerante. Documentação não é executada.

**Truncar na serialização JSON.** Corrigiria a aparência e deixaria dois valores diferentes
em memória e no banco — o defeito reapareceria em qualquer comparação interna.

**Coluna de maior precisão.** O Postgres não oferece: `TIMESTAMPTZ` é microssegundo, ponto.

## Consequências

- O que a aplicação produz e o que o banco devolve são o mesmo valor, sempre.
- Guardado por `InstantRoundTripIT`. Verificado por mutação: revertendo para
  `Clock.systemUTC()`, os testes falham com
  `expected ...291324069Z but was ...291324Z`.
- Esse é o **único** teste de integração que deliberadamente **não** usa relógio fixo. Um
  relógio congelado em segundo cheio não tem dígitos fracionários para perder, então a
  asserção passaria sem provar nada. Ele afirma uma propriedade do valor, não a identidade.
- Timestamps deixam de ser únicos sob concorrência alta dentro do mesmo microssegundo. Não
  afeta este sistema, onde nada usa timestamp como chave.
