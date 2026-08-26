# 0011 — Tratamento de erros centralizado, com identificador de rastreio

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

O desafio exige relatório de análise de vulnerabilidades, e vazamento de informação em
mensagem de erro é achado clássico de qualquer scanner.

Havia um vazamento **ativo**: os dois advices por fatia mapeavam `IllegalArgumentException`
para 400 **e copiavam a mensagem para o corpo**. Como o domínio lança essa exceção com
mensagens internas — `"vehicleId must not be null"`, `"field must not be blank"` — essas
invariantes já eram publicadas para quem pedisse.

E o caso específico da placa: o Postgres relata violação do índice único como

```
duplicate key value violates unique constraint "ux_vehicles_license_plate_active"
Detail: Key (license_plate)=(ABC1D23) already exists.
```

O nome do índice **e** a placa, na mesma string.

## Decisão

Um `@RestControllerAdvice` global estendendo `ResponseEntityExceptionHandler`, com
`ProblemDetail` (RFC 9457). O cliente recebe status, código estável, uma frase escrita para
humano e o identificador de rastreio — **e nada mais**.

**A distinção em que tudo se apoia:**

| Tipo | Mensagem vai ao cliente? |
|---|---|
| `BusinessException` | **Sim.** É escrita para o cliente, em pt-BR, sem valor submetido |
| Todo o resto | **Não.** Resposta genérica; a mensagem real vai só para o log |

**O `traceId` é o mecanismo central.** `TraceIdFilter` gera um UUID por requisição, põe no
MDC, devolve no header `X-Trace-Id` e repete no corpo. O cliente recebe identificador
opaco, o servidor guarda o detalhe. Suporte correlaciona; atacante não aprende nada. **Não**
é lido de header de entrada: valor escolhido pelo cliente permitiria colidir com — ou
forjar — as linhas de log de outra pessoa.

**Catálogo `ErrorCode`**, com o status no código e **nenhum `@ResponseStatus` em lugar
nenhum**: com a anotação, "o que esta falha responde" fica espalhado por tantos arquivos
quantas exceções existem, e duas exceções que significam a mesma coisa divergem sem ninguém
notar.

**Log:** 4xx em `WARN` sem stack trace — cliente mandando entrada ruim não é incidente, e
uma stack por requisição malformada esconde os 5xx que importam. 5xx em `ERROR` com stack
completa. Toda linha carrega o `traceId`.

## Alternativas consideradas

**Advice por fatia.** É o estado anterior, e é a estrutura que reintroduz o vazamento: a
regra precisa ser dita **uma vez** para valer em todo lugar.

**Devolver `getMessage()` das exceções de domínio.** Conveniente e é o que vazava.

**Status 422 para placa e CPF inválidos**, como a especificação da tarefa sugeria.
Recusado, e é um **desvio deliberado**: o mesmo campo já responde 400 quando chega vazio,
pela bean validation. Exigir que o cliente trate 400 para "faltando" e 422 para "formato
errado" no mesmo campo é pedir que trate duas vezes a mesma coisa. O 422 se justifica
quando a requisição é válida isoladamente mas conflita com o estado — e esse caso já é 409.

## Consequências

- Toda resposta de erro tem `code` e `traceId`, **inclusive 401 e 403**, que vêm do filtro
  de segurança e não do advice. Um contrato que vale para todo erro menos os dois mais
  comuns não é contrato.
- O 401 não distingue "usuário não existe" de "senha errada" de "token expirado": a
  diferença é um oráculo de enumeração.
- Um defeito foi encontrado pelos próprios testes: `handleExceptionInternal` montava o
  corpo com o status do catálogo e devolvia o `ResponseEntity` com o status real. Rota
  inexistente saía com **HTTP 404 e corpo dizendo `"status":400`**.
- Os testes são escritos como **proibições**, não expectativas. Um teste que verifica se o
  409 tem a mensagem certa fica verde enquanto o corpo também carrega o nome do índice e a
  placa; só um teste que afirma sobre ausência pega isso.
- Diagnóstico depende do log. Sem acesso a ele, o `traceId` não serve para nada — o que é
  exatamente o desenho pretendido.
