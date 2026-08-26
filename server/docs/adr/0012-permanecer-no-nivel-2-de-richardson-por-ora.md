# 0012 — Permanecer no nível 2 de Richardson, por prioridade e não por impedimento

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

A Tarefa 6 pedia um spike de viabilidade do Spring HATEOAS, com critério de abandono
explícito, motivado por duas incompatibilidades documentadas entre springdoc-openapi e
Spring HATEOAS no Spring Boot 4:

- **springdoc #3238** (março/2026): o Boot 4 removeu `HateoasProperties`, mas o SpringDoc
  3.0.2 ainda a referencia em `SpringDocHateoasConfiguration`, causando
  `ClassNotFoundException` **na inicialização**.
- **springdoc #3095**: falha ao registrar o módulo HAL, com `NoClassDefFoundError` em
  `Jackson2HalModule`, porque o HATEOAS 3.x migrou para Jackson 3.

A falha, se ocorresse, impediria a aplicação de subir — e o Swagger é entregável
obrigatório do desafio, enquanto HATEOAS não é cobrado em lugar nenhum do enunciado.

Estamos no springdoc 3.0.3, um patch acima do relatado.

## Decisão

O spike foi executado. **Nenhuma das duas incompatibilidades reproduz.**

| Verificação | Resultado |
|---|---|
| `spring-boot-starter-hateoas` sem tag `version` | resolve para `spring-hateoas:3.1.1` |
| Aplicação sobe | sim |
| `GET /v3/api-docs` | 200 |
| `GET /swagger-ui/index.html` | 200 |
| `HateoasProperties` / `ClassNotFoundException` no log | **zero ocorrências** |
| `Jackson2HalModule` / `NoClassDefFoundError` no log | **zero ocorrências** |
| Suíte completa (175 unitários + 47 integração) | verde, sem regressão |

**Versões medidas:** Java 25.0.4, Spring Boot 4.1.0, springdoc-openapi 3.0.3,
spring-hateoas 3.1.1, Hibernate 7, Postgres 16.

As duas issues foram corrigidas entre a 3.0.2 relatada e a 3.0.3 em uso.

**Ainda assim, permanecemos no nível 2 por ora.** O motivo é prioridade, não impedimento:
JaCoCo e o relatório de análise de vulnerabilidades são **entregáveis obrigatórios** e
seguem sem configuração alguma, enquanto hipermídia não é cobrada. Este ADR deve ser
substituído — não editado — quando o grupo decidir implementar.

## Alternativas consideradas

**Implementar agora em `vehicle`.** Custo revisado para ~200 linhas, e o spike prova que é
seguro. Adiado apenas pela ordem de prioridade acima.

**Implementar em `ServiceOrder`, como a tarefa original pedia.** Inviável hoje: aquela
fatia tem o agregado com as sete transições, mas **nenhuma API** — sem controller, sem
repositório, sem casos de uso. Seriam ~1.300 linhas de fatia antes de qualquer hipermídia,
84% do esforço em construir código que não é HATEOAS, e passando por decisões de modelagem
que pertencem ao dono daquela fatia.

**Abandonar definitivamente.** Recusado: o spike mostrou que não há impedimento técnico, e
registrar "não dá" seria falso.

## Consequências

- A dependência foi **revertida**. Reintroduzir é uma linha no `pom`, sem tag `version`.
- **Achado que fica registrado**, e que reduz o custo estimado: usando `EntityModel` em vez
  de `CollectionModel`, os campos são serializados inline e apenas ganham `_links` ao lado.
  O contrato de [0010](0010-contrato-proprio-de-paginacao.md) sobrevive intacto —
  `$.content[0].id`, `$.page`, `$.totalElements` continuam válidos — e não é preciso adotar
  o `PagedModel` do HATEOAS, cuja ambiguidade de nome aquele ADR existe para evitar.
- **Limitação que precisa ser dita**, caso `vehicle` seja o alvo escolhido: os links de um
  veículo isolado **não variam**. A única transição do agregado é ativo → removido, e um
  veículo removido não é devolvido pela API. `update` e `remove` estariam sempre presentes,
  então o que uma banca de DDD procura — link condicional dirigido por predicado do domínio
  — não se demonstra ali. O valor real em `vehicle` está nos links de paginação da coleção,
  que variam de verdade.
- Um detalhe de ambiente encontrado durante o spike: o mirror corporativo configurado em
  `~/.m2/settings.xml` estava inacessível por DNS. A resolução foi feita direto do Maven
  Central, com `settings.xml` temporário e sem alterar a configuração do usuário.
