# 0002 — Checkstyle restrito a três regras de import

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Três decisões já tomadas pelo grupo não têm como ser verificadas por um formatador, porque
não são questão de forma:

1. O Spring Boot 4 serializa com Jackson 3 (`tools.jackson`), mas o Jackson 2
   (`com.fasterxml.jackson`) permanece no classpath por transitividade do springdoc e do
   jjwt. Importar a classe errada **compila e passa nos testes** — só falha quando o objeto
   configurado por engano nunca é consultado pelo `ObjectMapper` em uso.
2. `java.util.Date` e `java.sql.Timestamp` carregam fuso implícito da máquina, e a
   aplicação mede tempo médio de execução de serviço.
3. O Lombok foi removido de propósito: o `@ToString` gerado imprimia a placa íntegra em
   log, contra o requisito de mascaramento.

Os três são erros silenciosos, que é a classe que vale automatizar.

## Decisão

`maven-checkstyle-plugin` com **exatamente três regras `IllegalImport`**, na fase
`validate`, cada uma com mensagem própria explicando o motivo:

| Import barrado | Use no lugar |
|---|---|
| `com.fasterxml.jackson.databind` | `tools.jackson.databind` |
| `java.util.Date`, `java.sql.Timestamp` | `java.time.Instant` |
| `lombok.*` | código escrito à mão |

`com.fasterxml.jackson.annotation` **continua liberado**: as anotações são compartilhadas
pelas duas linhas do Jackson por decisão do próprio projeto Jackson.

Complementarmente, `maven-enforcer-plugin` reprova `com.fasterxml.jackson.core` como
dependência **direta** em escopo `compile`, com busca **não-transitiva**. O alvo não é a
presença do jar — bani-lo custaria o Swagger — é a declaração deliberada.

## Alternativas consideradas

**Conjunto amplo de regras Checkstyle.** Recusado: duas ferramentas opinando sobre o mesmo
assunto geram build vermelho sem informação nova, e o Spotless já reescreve o arquivo em
vez de reclamar dele. Checkstyle aqui cobre só o que o formatador não enxerga.

**Revisão humana.** É o que já falha: o import errado passa despercebido justamente porque
compila.

**Enforcer transitivo.** Reprovaria o Jackson 2 que vem do springdoc, custando o Swagger,
que é entregável obrigatório.

## Consequências

- Introduzir qualquer um dos três imports reprova o build com mensagem que explica a
  alternativa.
- Uma exceção documentada existe: a jjwt 0.13 exige `java.util.Date` na assinatura da
  própria API, registrada em `config/checkstyle/suppressions.xml` com critério de saída.
- Uma quarta regra exigiria decisão do grupo. O escopo estreito é o que mantém o
  Checkstyle sem opinião sobre estilo.
