# 0010 — Contrato próprio de paginação, com lista branca de ordenação

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Três formatos de página conviviam — `VehiclePage`, `VehiclePageResponse` e `PageResponse` —
e mais endpoints de listagem estavam por vir.

Havia também um problema de segurança **ativo**: a fatia `customer` recebia `Pageable` do
binding e o passava até dentro do service, com `sort` arbitrário e sem lista branca. Um
`?sort=qualquerCoisa` produz `PropertyReferenceException`, cuja mensagem **enumera as
propriedades que existem** na entidade — mapeia o schema uma requisição por vez. E page
size sem teto é vetor de negação de serviço.

## Decisão

Contrato único em três camadas:

| Camada | Tipos | Regra |
|---|---|---|
| Aplicação | `PageQuery`, `PageResult<T>`, `SortCriterion` | **não importa `org.springframework.data`** |
| Web | `PageParameters` (`@ParameterObject`), `SortableFields` | valida limites e lista branca |
| Infraestrutura | `SpringDataPaging` | único ponto que converte para `Pageable`/`Page` |

**`PageResult`, e nunca `PagedModel`.** Esse nome já existe em
`org.springframework.data.web` **e** em `org.springframework.hateoas`; um terceiro tornaria
todo import ambíguo no momento em que hipermídia entrasse.

**Nunca devolver `Page<T>` do Spring Data.** O formato JSON dele é consequência acidental
dos campos Java — traz `pageable`, `numberOfElements`, `first`, `last` e um `sort`
aninhado — e muda com upgrade de framework.

**Tamanho acima de 100 é recusado, não truncado.** Devolver 100 elementos em silêncio a
quem pediu 500 faz o laço do cliente pular quatro quintos dos dados e **reportar sucesso**.
Resposta errada é pior que erro, porque só o erro é corrigido.

**Lista branca por recurso**, e a mensagem de erro não repete o campo submetido. `taxId`
fica fora da lista de clientes: ordenar por documento permite busca binária sobre os CPFs
cadastrados sem ler registro nenhum, porque as fronteiras de página revelam os valores.

**Desempate determinístico:** toda ordenação termina no identificador. Sem ele, linhas de
mesmo valor voltam numa ordem que o banco pode escolher diferente entre duas consultas, e
um registro aparece na página 1 e de novo na 2, ou em nenhuma.

## Alternativas consideradas

**`Pageable` direto no controller.** É o estado que produziu o vazamento.

**Lista negra de campos proibidos.** Falha por omissão: um campo novo na entidade nasce
ordenável.

**Truncar o tamanho no teto.** Ver acima.

## Consequências

- A camada de aplicação pode ser testada sem Spring Data no classpath.
- O nome do campo na API não precisa casar com o da persistência: a resposta de veículo diz
  `registeredAt` enquanto a propriedade JPA diz `createdAt`, e o adaptador declara o mapa.
- **Limite de teste que fica registrado:** `PagingContractIT` verifica que percorrer as
  páginas devolve cada registro exatamente uma vez, mas **continua verde se o desempate for
  removido** — verificado por mutação. O Postgres devolve ordem estável por conta própria
  numa tabela pequena; instabilidade é *permitida*, não garantida, então nenhum teste de
  integração consegue forçá-la. Quem guarda o desempate é
  `PageParametersTest.appendsTheTieBreaker`, que afirma sobre os critérios resolvidos.
- `CustomerService` cumpre a letra da regra, não o espírito: não importa
  `org.springframework.data`, mas recebe `Page<Customer>` por inferência e chama
  `SpringDataPaging`. A regra pressupõe uma separação application/infrastructure que aquela
  fatia não tem.
