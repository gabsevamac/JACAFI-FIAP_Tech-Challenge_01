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
