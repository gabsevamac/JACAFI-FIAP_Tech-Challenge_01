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

### Serialização JSON

ISO-8601 com sufixo `Z`, que é o comportamento **default** do Jackson 3 no Boot 4.
Verificado num teste de controller antes de configurar qualquer coisa; nenhuma
configuração foi necessária.

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
