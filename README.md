# SINATES — Sistema Integrado de Atendimento e Execução de Serviços

Back-end (MVP) do sistema de gestão de uma oficina mecânica de médio porte, desenvolvido como Tech Challenge da Fase 1 da pós-graduação em Arquitetura de Software.

> **Idioma:** documentação em português, código-fonte em inglês. O mapeamento entre os dois está fixado em [`docs/linguagem-ubiqua.md`](docs/linguagem-ubiqua.md), §9.

---

## O problema

Uma oficina mecânica especializada em manutenção de veículos gerencia atendimento, diagnóstico, execução e entrega por anotações manuais e planilhas. Isso produz erros de priorização, falhas no controle de peças, dificuldade de acompanhar o andamento dos serviços, perda de histórico e ineficiência no fluxo de orçamentos.

O ponto que organiza tudo: **cada intervenção envolve dinheiro que não é da oficina e um bem que não é da oficina.** A autorização do cliente é, portanto, a condição de legitimidade de todo trabalho executado — e hoje ela circula sem rastro.

O SINATES existe para tornar o ciclo `diagnóstico → orçamento → aprovação → execução` auditável e visível ao cliente em tempo real, de modo que nenhum serviço seja executado sem consentimento registrado e nenhuma peça saia do estoque sem vínculo com uma ordem aprovada.

---

## Modelagem

O projeto aplica Domain-Driven Design. A documentação de domínio precede o código e é normativa: divergências entre código e documentação são tratadas como defeito.

| Documento | Conteúdo |
|---|---|
| [`docs/linguagem-ubiqua.md`](docs/linguagem-ubiqua.md) | Dicionário da linguagem ubíqua, destilação, de/para PT→EN |
| [`docs/event-storming.md`](docs/event-storming.md) | Event Storming (nível 3), agregados e fronteiras |

### Contexto delimitado

Único contexto no MVP: **Gestão de Ordens de Serviço**.

Não pertencem a este contexto: faturamento fiscal, folha de pagamento, compras e fornecedores, agendamento de horários.

### Destilação

| Categoria | Escopo |
|---|---|
| **Núcleo** | Ciclo de vida da ordem de serviço com a aprovação como portão |
| **Apoio** | Catálogo de serviços, estoque de peças e insumos |
| **Genérico** | Cadastro de clientes e veículos, validação documental, autenticação |

Todos serão implementados — a destilação orienta **onde investir esforço de teste e modelagem**, não o que existe. A cobertura mínima de 80% é obrigatória no núcleo, aplicada conforme risco no apoio.

### Agregados

| Agregado | Raiz | Responsável |
|---|---|---|
| `ServiceOrder` | Ordem de serviço, com diagnóstico, orçamento e itens lançados | *a definir* |
| `Customer` | Cliente | *a definir* |
| `Vehicle` | Veículo | *a definir* |
| `Inventory` | Estoque | *a definir* |

A invariante central — **nenhum serviço executado sem aprovação registrada** — não tolera janela de inconsistência. Por isso `Estimate` é entidade interna a `ServiceOrder`, e não agregado próprio. `Inventory` é agregado separado porque a consistência com a ordem de serviço admite atraso; a ligação entre eles é feita exclusivamente por política.

---

## Arquitetura

**Monolito em camadas, organizado por fatias verticais (vertical slice).** Cada agregado é uma fatia autocontida sob a responsabilidade de um integrante do grupo.

```
src/main/<lang>/<base-package>/
├── vehicle/            # fatia — agregado Vehicle
│   ├── domain/         # entidades, objetos de valor, regras, portas
│   ├── application/    # casos de uso
│   ├── infrastructure/ # persistência, adaptadores
│   └── api/            # controllers, DTOs
├── customer/
├── serviceorder/
├── inventory/
└── shared/             # kernel compartilhado — apenas o mínimo
```

### Regras de fronteira entre fatias

1. Uma fatia **nunca** importa classes de `domain/` ou `infrastructure/` de outra fatia.
2. Referências entre agregados são feitas **por identificador**, nunca por referência de objeto — `Vehicle` guarda um `CustomerId`, não um `Customer`.
3. Comunicação entre fatias ocorre por evento de domínio ou por porta explícita declarada em `shared/`.
4. `shared/` contém apenas o que é genuinamente transversal: tipos base, tratamento de erro, utilitários de segurança. **Não é depósito de conveniência.**

Dentro de cada fatia adota-se Clean Architecture, com dependências apontando para o domínio. A profundidade da implementação é proporcional à complexidade da fatia: uma fatia de cadastro não precisa da mesma cerimônia que a fatia do núcleo.

---

## Proteção de dados

O sistema trata **dados pessoais** na acepção do Art. 5º I da LGPD (Lei nº 13.709/2018): nome, CPF/CNPJ, contato e placa de veículo quando vinculável a pessoa identificada.

> **Correção terminológica:** o enunciado do desafio chama CPF, CNPJ e placa de "dados sensíveis". Na LGPD, dado sensível é **categoria fechada** (Art. 5º II: origem racial ou étnica, convicção religiosa, opinião política, filiação sindical, dados referentes à saúde ou vida sexual, dado genético ou biométrico). Os dados tratados por este sistema são **pessoais, não sensíveis**. A distinção altera a base legal aplicável e o regime de tratamento.

Medidas adotadas:

| Princípio / direito | Implementação |
|---|---|
| Necessidade (Art. 6º III) | Persistir apenas os campos exigidos pelo caso de uso |
| Segurança (Art. 6º VII, Art. 46) | Autenticação JWT nas APIs administrativas; dados pessoais nunca em log, mensagem de erro ou stack trace |
| Transparência (Art. 6º VI) | Campos de dado pessoal explicitamente anotados no código |
| Direitos do titular (Art. 18) | Endpoints de consulta e correção; exclusão por anonimização |
| Limitação de retenção (Art. 16) | Exclusão lógica com anonimização, preservando o histórico de serviço exigido por obrigação legal e por garantia (Art. 16 I) |
| Registro de tratamento (Art. 37) | Trilha de auditoria com autor, momento e operação |

Placas e documentos aparecem mascarados em qualquer saída de log.

---

## Requisitos técnicos

- Back-end monolítico em camadas
- APIs RESTful documentadas via OpenAPI/Swagger
- Autenticação JWT nas APIs administrativas
- Validação de CPF, CNPJ e placa (padrão brasileiro antigo e Mercosul)
- Testes automatizados com cobertura mínima de 80% nos domínios críticos
- `Dockerfile` para build da aplicação
- `docker-compose.yml` para orquestração do ambiente completo

### Banco de dados

*A justificativa da escolha é entregável obrigatório e deve ser preenchida aqui, cobrindo: modelo de dados dos agregados, requisitos transacionais da invariante de aprovação, e adequação ao volume esperado.*

---

## Tempo: UTC e o relógio injetado

Um dos requisitos do desafio é monitorar o tempo médio de execução dos serviços. Isso
é subtração entre timestamps, e subtração entre timestamps só é confiável se todas as
camadas concordarem sobre o que "agora" significa.

### UTC em toda camada

| Camada | Onde | O quê |
|---|---|---|
| Container da aplicação | `server/Dockerfile` | `ENV TZ=UTC` |
| Container do banco | `docker-compose.yaml` | `TZ=UTC` (SO) e `PGTZ=UTC` (sessão libpq) |
| JDBC / Hibernate | `application.yaml` | `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` |
| JVM | `ApplicationTimeZone.enforceUtc()`, chamado no `main` | cobre quem roda pela IDE, fora do Docker |

O domínio fala `Instant`. `LocalDateTime`, `java.util.Date` e `java.sql.Timestamp` são
barrados pelo Checkstyle — o primeiro por convenção do projeto, os outros dois por
regra de build.

### O relógio é injetado, nunca lido

Nenhuma classe chama `Instant.now()`. Todas recebem o bean `Clock` de
`shared/time/TimeConfiguration` e chamam `Instant.now(clock)`.

Isso não é purismo: é o que torna o teste de tempo uma igualdade em vez de uma
tolerância. Um teste que afirma "`createdAt` é aproximadamente agora" passa na máquina
rápida e falha na CI carregada. Com `FixedClockConfiguration`, a asserção é `isEqualTo`.

```java
@Import(FixedClockConfiguration.class)   // congela em 2026-01-15T10:30:00Z
class AlgumaCoisaIT extends AbstractIntegrationTest { ... }
```

### Precisão: microssegundos, não nanossegundos

O bean `Clock` é `Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))`, e não
`Clock.systemUTC()`. O motivo foi medido no endpoint de veículos — o mesmo campo do
mesmo recurso voltava de duas formas:

```
POST  →  "registeredAt":"2026-08-26T14:20:48.492948227Z"
GET   →  "registeredAt":"2026-08-26T14:20:48.492948Z"
```

`Clock.systemUTC()` resolve em nanossegundos, `TIMESTAMPTZ` do Postgres guarda
microssegundos, e os três dígitos excedentes sumiam na escrita sem aviso. Um cliente
que guardasse a resposta da criação e comparasse com uma leitura posterior veria
divergência num recurso que ninguém alterou. Arredondar na origem elimina a
discrepância: a aplicação passa a produzir exatamente o que o banco consegue guardar.

`InstantRoundTripIT` guarda essa decisão — reverter para `Clock.systemUTC()` faz os
dois testes falharem.

### Colunas: `TIMESTAMP WITH TIME ZONE`, sem exceção

Toda coluna de tempo do schema carrega o fuso. `DatabaseMigrationTest` consulta o
`information_schema` e falha se qualquer tabela nossa tiver uma coluna
`timestamp without time zone` — a verificação é do schema inteiro, então uma fatia nova
que criar `created_at TIMESTAMP` é barrada sem depender de alguém lembrar da regra na
revisão.

O tipo sem fuso guarda hora de parede sem procedência: quem escreveu decidiu o fuso, a
coluna não registrou qual foi, e não há como descobrir depois.

### Serialização JSON

ISO-8601 com sufixo `Z`, que é o comportamento **default** do Jackson 3 no Boot 4.
Verificado num teste de controller antes de configurar qualquer coisa; nenhuma
configuração foi necessária.

Com a troca de `LocalDateTime` por `Instant`, o JSON de `customers` passou a incluir o
sufixo — e só então passou a cumprir o contrato acima:

```
antes   "createdAt":"2026-01-15T10:30:00.123456"
depois  "createdAt":"2026-01-15T10:30:00.123456Z"
```

---

## Tratamento de erros

RFC 9457 (`application/problem+json`), um `@RestControllerAdvice` global, e uma regra que
não tem exceção.

### O que o cliente recebe

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Placa já cadastrada para outro veículo ativo.",
  "instance": "/api/v1/vehicles",
  "code": "VEI-002",
  "traceId": "8aab65dd-a531-4963-bc39-e31c456992b2"
}
```

Status, código estável, uma frase escrita para humano, e o identificador de rastreio. E
**nada mais** — nunca stack trace, SQL, nome de constraint ou de índice, nome de classe ou
propriedade, caminho de arquivo, nem o valor que o cliente submeteu.

### O traceId é o mecanismo central

`TraceIdFilter` gera um UUID por requisição, coloca no MDC do log, devolve no header
`X-Trace-Id` e repete no corpo. O cliente recebe um identificador opaco; o log do servidor
tem o detalhe completo. Suporte correlaciona, atacante não aprende nada.

O identificador **não** é lido de header de entrada: aceitar um valor escolhido pelo
cliente permitiria colidir com — ou forjar — as linhas de log de outra pessoa.

O filtro tem `HIGHEST_PRECEDENCE` porque uma falha num filtro posterior, como um JWT
ilegível, também precisa sair com o header, e um filtro que ainda não rodou não consegue
acrescentá-lo.

### Catálogo de códigos

`ErrorCode` é a lista inteira, numa tela. Um código existe para o cliente ramificar sem
casar com texto — texto é reescrito, traduzido e corrigido; `VEI-002` não.

| Prefixo | Fatia |
|---|---|
| `GEN-00x` | genéricos |
| `SEG-00x` | autenticação e autorização |
| `PAG-001` | paginação e ordenação |
| `VEI-00x` | veículos |
| `CLI-00x` | clientes |

O status vive no código, e **não existe `@ResponseStatus` em lugar nenhum**: com a
anotação, "o que esta falha responde" fica espalhado por tantos arquivos quantas exceções
existem, e duas exceções que significam a mesma coisa divergem sem ninguém notar.

### A distinção em que tudo se apoia

| Tipo | Mensagem vai ao cliente? |
|---|---|
| `BusinessException` | **Sim.** É escrita para o cliente, em pt-BR, sem valor submetido |
| Todo o resto | **Não.** Resposta genérica; a mensagem real vai só para o log |

Isso corrige um vazamento que estava ativo: os dois advices por fatia mapeavam
`IllegalArgumentException` para 400 **e copiavam a mensagem para o corpo**, publicando
invariantes de domínio como `"vehicleId must not be null"` para quem pedisse.

`BusinessException` também aceita um `logContext` — o identificador do registro, por
exemplo — que vai para o log e nunca para a resposta.

### O caso da placa duplicada

O Postgres relata violação do índice único assim:

```
duplicate key value violates unique constraint "ux_vehicles_license_plate_active"
Detail: Key (license_plate)=(ABC1D23) already exists.
```

O nome do índice **e** a placa, na mesma string. O Hibernate embrulha, o Spring reembrulha,
e qualquer handler que chame `getMessage()` publica os dois: o schema, e um dado pessoal
que o chamador talvez não tivesse direito de confirmar. Por isso `DataIntegrityViolation`
é respondido genericamente, com a mensagem completa indo só para o log.

### Log

| Faixa | Nível | Stack trace |
|---|---|---|
| 4xx | `WARN` | não — cliente mandando entrada ruim não é incidente, e uma stack por requisição malformada esconde os 5xx que importam |
| 5xx | `ERROR` | sim, completa |

Toda linha carrega o `traceId` pelo padrão de log configurado no `application.yaml`. Sem
isso o identificador na resposta não serviria para nada.

### Endurecimento do `application.yaml`

`server.error.include-stacktrace/message/binding-errors=never`, `include-exception=false`,
`whitelabel.enabled=false` e `spring.jpa.show-sql=false`. Todos já são default do Boot;
ficam explícitos porque são decisão de segurança, e um default silencioso pode mudar entre
versões sem ninguém notar.

### Os testes são o entregável

`ErrorLeakageIT` e `UnhandledExceptionTest` são escritos como **proibições**, não como
expectativas. Um teste que verifica se o 409 tem a mensagem certa continua verde enquanto
o corpo também carrega o nome do índice e a placa submetida; só um teste que afirma sobre
ausência pega isso.

`UnhandledExceptionTest` usa um controller que existe só no teste e cuja única função é
lançar `NullPointerException` — forçar 500 num endpoint real exigiria quebrá-lo de
propósito ou encontrar um defeito, e nenhum dos dois dá teste repetível.

---

## Paginação

Contrato único para toda listagem da API — veículos, clientes, ordens de serviço, peças.

### Requisição

```
GET /api/v1/vehicles?customerId=...&page=0&size=20&sort=registeredAt,desc
```

| Parâmetro | Default | Regra |
|---|---|---|
| `page` | `0` | Base zero. Negativo → **400** |
| `size` | `20` | Máximo **100**. Acima → **400**, nunca truncado em silêncio |
| `sort` | identificador | `campo` ou `campo,asc` / `campo,desc`. Repetível. Fora da lista branca → **400** |

### Resposta

```json
{
  "content": [ { "id": "…", "licensePlate": "ABC1D23", "make": "Volkswagen" } ],
  "page": 0,
  "size": 20,
  "totalElements": 37,
  "totalPages": 2
}
```

### As quatro decisões

**`PageResult`, e nunca `PagedModel`.** Esse nome já existe em
`org.springframework.data.web` **e** em `org.springframework.hateoas`. Um terceiro tornaria
todo import ambíguo no momento em que hipermídia entrasse.

**Nunca devolver `Page<T>` do Spring Data.** O formato JSON dele é consequência acidental
dos campos Java da classe — traz `pageable`, `numberOfElements`, `first`, `last` e um
`sort` aninhado — e muda com upgrade de framework. O próprio Spring Data desaconselha
serializá-lo.

**Tamanho acima do teto é recusado, não truncado.** Devolver 100 elementos em silêncio a
quem pediu 500 faz o laço do cliente pular quatro quintos dos dados e reportar sucesso.
Resposta errada é pior que erro, porque só o erro é corrigido.

**Lista branca de campos ordenáveis, por recurso.** Passar a string do cliente direto para
o Spring Data deixa ele resolver qualquer propriedade da entidade — `?sort=passwordHash`
ordena por uma coluna que a API não expõe, e a `PropertyReferenceException` resultante
**nomeia as propriedades que existem**. A lista branca responde a toda sondagem do mesmo
jeito, e a mensagem de erro não repete o campo submetido.

`taxId` está fora da lista de clientes de propósito: ordenar por ele permite fazer busca
binária sobre os documentos cadastrados sem nunca ler um — as fronteiras de página
revelam os valores.

### Desempate determinístico

Toda ordenação termina no identificador. Ordenar só por coluna não única deixa linhas de
mesmo valor numa ordem que o banco pode escolher diferente entre duas consultas, e aí um
registro aparece na página 1 e de novo na 2, ou em nenhuma.

Vale registrar o limite do teste disso: `PagingContractIT` verifica que percorrer as
páginas devolve cada registro exatamente uma vez, mas **continua verde se o desempate for
removido** — o Postgres devolve ordem estável por conta própria numa tabela pequena.
Instabilidade é permitida, não garantida, então nenhum teste de integração consegue
forçá-la. Quem guarda o desempate de fato é `PageParametersTest.appendsTheTieBreaker`, que
afirma sobre os critérios resolvidos e falha sob essa mutação.

### Onde cada tipo mora

| Camada | Tipos | Regra |
|---|---|---|
| Aplicação (`shared/application`) | `PageQuery`, `PageResult<T>`, `SortCriterion` | **Não importa `org.springframework.data`** |
| Web (`shared/web`) | `PageParameters` (`@ParameterObject`), `SortableFields` | Valida limites e lista branca |
| Infraestrutura (`shared/infrastructure/persistence`) | `SpringDataPaging` | Único ponto que converte para `Pageable`/`Page` |

O nome do campo na API não precisa casar com o da persistência: a resposta de veículo diz
`registeredAt` enquanto a propriedade JPA diz `createdAt`, e o adaptador declara esse mapa.

---

## Auditoria

Duas coisas diferentes, que costumam ser confundidas por terem o mesmo nome.

### Auditoria técnica — `shared/persistence/AuditableEntity`

`@MappedSuperclass` herdada por toda entidade de negócio, com `createdAt`/`createdBy`,
`updatedAt`/`updatedBy`, `deletedAt`/`deletedBy` e `version`. Responde *"o estado atual
desta linha veio de onde"*.

**Os nomes das colunas são idênticos em toda tabela**, e isso é o ponto da superclasse.
`vehicles` chamava `registered_at` e `removed_at`; passou a usar `created_at` e
`deleted_at` como as demais. O vocabulário de negócio continua vivo no domínio —
`Vehicle.getRegisteredAt()`, o evento `VehicleRemoved` — e a ponte fica no mapper de
persistência, que é o único lugar obrigado a conhecer os dois.

Os valores vêm do `AuditingEntityListener` alimentado pelo bean `Clock`, via
`ClockDateTimeProvider`. Sem isso a auditoria seria a única parte do sistema a ignorar o
relógio injetado, e o teste só poderia afirmar "aproximadamente agora".

O autor vem do `JwtAuditorAware`, que lê o subject do JWT do `SecurityContext`. Sem
autenticação — migração, job, seed — grava `"system"`, nunca `null`. Uma coluna de autor
nulável obriga todo relatório de autoria a decidir o que `null` significa, e a decisão
usual é omitir a linha, o que transforma uma escrita sem autor numa escrita invisível.

### Remoção lógica: filtro explícito

A convenção é filtro explícito no repositório (`findByIdAndDeletedAtIsNull`), e não
`@SQLDelete` + `@SQLRestriction`. O comportamento das duas foi medido no Hibernate 7:

| Caminho de leitura | `@SQLRestriction` filtra? |
|---|---|
| `em.find()` por chave primária | sim |
| JPQL, Criteria, derived query | sim |
| **query nativa** | **não** |

O critério não foi capacidade — **nenhuma das duas filtra query nativa**. Foi a crença
que cada uma cria. Com `@SQLRestriction` a remoção lógica parece resolvida globalmente,
então uma query nativa devolvendo linha removida vira surpresa; com filtro explícito não
existe essa expectativa. Como o relatório de vulnerabilidades é entregável, e "listagem
devolveu registro removido" é achado clássico, a opção que falha de forma visível ganha
da que falha em silêncio.

O contra-argumento é real: filtro explícito depende de disciplina. Se as consultas se
multiplicarem, vale reavaliar.

### Bloqueio otimista

`version` existe para que duas telas abertas sobre a mesma ordem de serviço não gravem
uma por cima da outra. Num fluxo de aprovação de orçamento, a última escrita vencendo em
silêncio significa registrar aprovação de um total que o cliente não viu.

O adaptador de veículo **carrega a linha e copia estado para dentro dela**, em vez de
mesclar uma instância destacada recém-construída. Uma instância reconstruída apresenta
`version = 0` sempre, então a segunda gravação de uma linha colidiria com a primeira dela
mesma. A alternativa seria pôr `version` no agregado, fazendo o domínio carregar uma
preocupação de persistência.

### Trilha de auditoria — `shared/application/AuditTrailPort`

Tabela `audit_trail`, *append-only*, por campo: `aggregate_type`, `aggregate_id`,
`field_name`, `old_value`, `new_value`, `reason`, `changed_at`, `changed_by`. Responde
*"qual era o valor deste campo em tal data, e quem o mudou"* — que `updated_by` não
responde, porque guarda só o último autor e sobrescreve o anterior.

**A gravação é do caso de uso, não de um listener de JPA.** Um listener vê um valor mudar
e não sabe dizer *por quê* — e o porquê é o que importa, porque corrigir um erro de
digitação e registrar uma troca real de placa são os mesmos dois valores com significados
opostos. É o hot spot HS9, e só o caso de uso está em posição de responder.

`reason` é nulável por decisão, não por omissão: HS9 registra que a semântica do motivo
ainda é ambígua, e exigir preenchimento agora produziria um campo cheio de "atualização".

### ⚠️ `audit_trail` armazena dados pessoais

`old_value` e `new_value` guardam o valor **íntegro** do campo alterado, e entre os campos
auditados estão a placa do veículo e o CPF/CNPJ do cliente. Não são mascarados: uma trilha
registrando *"a placa mudou de \*\*\* para \*\*\*"* não responde a única pergunta que motiva
sua existência.

**Base legal da retenção: Art. 16, I** — conservação para cumprimento de obrigação legal
ou regulatória. O direito à eliminação do Art. 18, VI se ressalva expressamente às
hipóteses do Art. 16.

**Consequência assumida:** a remoção de veículo deixa de eliminar a placa do sistema. Ela
continua apagando `vehicles.license_plate`, substituindo-a por um token irreversível que
libera o índice único parcial para recadastro, mas o valor anterior permanece na trilha. O
histórico do valor da placa é requisito de negócio (HS7–HS10, mutabilidade de placa) e não
existe sem guardar o valor.

**O que isso obriga:**

- a tabela entra na política de retenção e precisa de prazo definido;
- um pedido de titular (Art. 18) alcança estas linhas e precisa de procedimento;
- os valores **nunca** podem ser logados nem devolvidos por API sem passar pelo `Masker`.

Nenhuma chave estrangeira liga a trilha às tabelas de negócio, de propósito: prova que
desaparece junto com o dado que descreve não é prova. O caráter *append-only* é regra de
aplicação — a porta só oferece `record` — e não restrição do schema; endurecer no banco
exigiria revogar `UPDATE` e `DELETE` do usuário da aplicação, o que fica registrado para
produção.

---

## Mascaramento de dados pessoais

`shared/lgpd/Masker` é a única fonte do formato. Placa, CPF/CNPJ e e-mail são dados
pessoais sob o Art. 5º, I da LGPD, e o Art. 6º, VII os mantém fora de log, mensagem de
erro e stack trace.

| Método | Entrada | Saída | Por que essa ponta |
|---|---|---|---|
| `licensePlate` | `ABC1D23` | `ABC****` | As três primeiras letras são comuns aos dois layouts brasileiros, então a máscara não revela se a placa é Mercosul ou antiga |
| `document` | `52998224725` | `********725` | Os dígitos iniciais do CPF correlacionam com a região emissora; os finais são dígitos verificadores, derivados do resto |
| `email` | `mariana@example.com` | `m***@example.com` | O domínio identifica a organização, não a pessoa. A parte local usa máscara de tamanho fixo, para não revelar o comprimento |

Regras que o `Masker` aplica e que valem conhecer:

- **Nada lança exceção.** Um utilitário de mascaramento que pode falhar acaba embrulhado
  em `try/catch` e eventualmente pulado — e o modo de falha do "pulado" é o valor íntegro
  no log.
- **Valor curto demais é mascarado por inteiro.** Abaixo de seis caracteres, manter três
  esconderia menos do que revela.
- **O run de asteriscos é limitado a 11.** A máscara é proporcional, o que preserva o
  comprimento — inofensivo para CPF e CNPJ, que já se distinguem por ele. Mas o token que
  substitui a placa na remoção tem 41 caracteres, e reproduzir esse comprimento gerava uma
  linha de log com 38 asteriscos.

As fatias `vehicle` e `customer` delegam a ele: `LicensePlate.mask`, `Cpf.masked` e
`Cnpj.masked` são invólucros de uma linha. Antes desta tarefa cada uma tinha o seu
formato, o que produzia um log onde a mesma pessoa aparecia de duas maneiras.

---

## Decisões de arquitetura

As padronizações descritas nas seções seguintes têm um registro por decisão em
[`server/docs/adr/`](server/docs/adr/), no formato de Nygard: contexto, decisão,
alternativas recusadas **com o motivo**, e consequências — inclusive o que piorou.

O README diz *o que* vale hoje. O ADR diz *por que*, e por que não a outra coisa. Quando as
duas informações moram no mesmo parágrafo, a segunda some na primeira revisão que enxuga o
texto.

---

## Padrão de código e guarda-corpos de build

Com quatro pessoas editando fatias paralelas, formatação divergente vira ruído de diff:
o *code review* passa a discutir indentação em vez de decisão de modelagem. A resposta
não é combinar um estilo, é remover a escolha do caminho.

### Formatação

O [Spotless](https://github.com/diffplug/spotless) reescreve todo arquivo Java com o
**palantir-java-format**, e roda como `spotless:check` na fase `validate` — código fora
do padrão reprova o build antes mesmo de compilar.

```bash
mvn spotless:apply    # formata
mvn spotless:check    # só verifica (é o que o build faz)
```

O `check` e não o `apply` no build é deliberado: um build que reescreve o código-fonte
sozinho altera o arquivo que a pessoa está editando no meio da edição.

**Escolha do formatador.** Foi medida, não presumida. Ambos os candidatos rodam no JDK
25 com Spotless 3.x, e nenhum precisou das flags `add-exports` que a documentação mais
antiga menciona. O desempate foi o tamanho da reformatação inicial: o palantir preserva
16 dos 89 arquivos existentes, o google-java-format em AOSP preserva 9.

Uma armadilha de versão vale registro: o Spotless **2.44.5 não funciona no JDK 25** —
estoura `NoSuchMethodError` em `Log$DeferredDiagnosticHandler.getDiagnostics()`, porque
o javac mudou a assinatura interna. A partir da 3.x acompanha. Não faça downgrade.

### Regras de import

O `maven-checkstyle-plugin` carrega exatamente **três** regras, em
`server/config/checkstyle/checkstyle.xml`. Ele não opina sobre nome de variável nem
comprimento de método — isso é trabalho do Spotless.

| Import barrado | Use no lugar | Por quê |
|---|---|---|
| `com.fasterxml.jackson.databind` | `tools.jackson.databind` | O Boot 4 serializa com Jackson 3. O Jackson 2 está no classpath só por transitividade, então importar a classe errada compila e passa nos testes — e falha em produção |
| `java.util.Date`, `java.sql.Timestamp` | `java.time.Instant` | Carregam o fuso da máquina, e a aplicação mede tempo médio de execução |
| `lombok.*` | código escrito à mão | O `@ToString` gerado imprimia a placa íntegra em log, contra o requisito de mascaramento |

O subpacote `com.fasterxml.jackson.annotation` **continua liberado**: as anotações são
compartilhadas pelas duas linhas do Jackson por decisão do próprio projeto.

Exceções ficam em `server/config/checkstyle/suppressions.xml`, cada uma com motivo e
critério de saída registrados. Hoje há uma: a jjwt exige `java.util.Date` na assinatura
da própria API.

### Dependências banidas

O `maven-enforcer-plugin` reprova `com.fasterxml.jackson.core` como dependência
**direta** em escopo `compile`. A busca é não-transitiva de propósito — o Jackson 2 vai
continuar no classpath vindo do springdoc e do jjwt, e bani-lo de vez custaria o Swagger,
que é entregável obrigatório. O alvo é a declaração deliberada: se alguém precisou
adicioná-lo para compilar, o `import` é que está errado.

### Histórico de autoria

O commit que aplicou a formatação ao repositório inteiro tocou quase todas as linhas.
Sem tratamento, o `git blame` atribuiria o código de todo mundo a quem rodou o
formatador. Ative o arquivo de exclusão **uma vez por clone**:

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

A configuração é local e não se propaga sozinha — cada integrante precisa rodar o
comando. A interface web do GitHub respeita o arquivo automaticamente.

---

## Execução local

*A preencher conforme a stack for definida — pré-requisitos, variáveis de ambiente, subida via `docker compose up`, URL do Swagger e comando de testes.*

---

## Equipe

| Nome | Discord | Fatia |
|---|---|---|
| | | |

---

## Referências

BRANDOLINI, A. *Introducing EventStorming*. Leanpub.

EVANS, E. *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley, 2003.

KHONONOV, V. *Learning Domain-Driven Design*. O'Reilly, 2021.

VERNON, V. *Implementing Domain-Driven Design*. Addison-Wesley, 2013.

BRASIL. Lei nº 13.709, de 14 de agosto de 2018. Lei Geral de Proteção de Dados Pessoais (LGPD).
