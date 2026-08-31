# JACAFI — Sistema de Gestão de Oficina

Back-end do Tech Challenge FIAP para gestão de clientes, veículos, catálogo de serviços, estoque e ordens de serviço.

## Arquitetura

O projeto é um monólito modular organizado por fatias verticais. Cada fatia aplica Clean Architecture: o domínio não depende de Spring, JPA ou HTTP; a aplicação expõe casos de uso e portas; os adaptadores implementam web e persistência.

```text
com.jacafi.tech/<slice>/
├── domain/       # regras e entidades de negócio
├── application/  # casos de uso e portas
├── adapter/in/   # API REST, DTOs e controllers
├── adapter/out/  # JPA e integrações externas
└── config/       # composição da fatia quando necessária
```

Fatias atuais: `auth`, `customer`, `vehicle`, `inventory`, `servicecatalog` e `serviceorder`. Itens compartilhados são limitados a preocupações transversais, como segurança, auditoria, erros, paginação e tempo.

As decisões arquiteturais estão em [docs/adr](docs/adr), os termos do domínio em [docs/linguagem-ubiqua.md](docs/linguagem-ubiqua.md) e os fluxos em [docs/event-storming.md](docs/event-storming.md).

## Execução local

Pré-requisitos: Docker e Docker Compose. Para executar fora do container, use Java 25 e Maven 3.9+.

```powershell
docker compose --env-file .env.example up -d --build
```

Antes de subir, altere `JWT_SECRET` em `.env` por um segredo local de pelo menos 32 bytes. Os serviços ficam disponíveis em:

| Recurso | Endereço |
|---|---|
| API | `http://localhost:8082` |
| Swagger UI | `http://localhost:8082/swagger-ui/index.html` |
| OpenAPI | `http://localhost:8082/v3/api-docs` |
| Liveness | `http://localhost:8082/actuator/health/liveness` |
| Readiness | `http://localhost:8082/actuator/health/readiness` |

`APP_PORT` e `DATABASE_PORT` permitem alterar somente as portas expostas no host; `SERVER_PORT` mantém a porta interna da aplicação. O Compose aguarda a saúde do PostgreSQL antes de iniciar a API e verifica a prontidão da aplicação.

A imagem de runtime desabilita OpenAPI e Swagger por padrão. O Compose os habilita somente para desenvolvimento local; em outro ambiente, mantenha `SPRINGDOC_API_DOCS_ENABLED=false` e `SPRINGDOC_SWAGGER_UI_ENABLED=false`, ou proteja os endpoints por rede e autenticação.

Para habilitar os e-mails de mudança de status da OS, preencha somente o seu `.env` local com `RESEND_ENABLED=true`, `RESEND_API_KEY` e `RESEND_FROM`. A chave não é versionada nem registrada em logs. O remetente deve pertencer a um domínio verificado no Resend; mantenha o recurso desabilitado quando não houver credenciais.

Para encerrar o ambiente:

```powershell
docker compose down
```

Use `docker compose down --volumes` apenas quando for necessário recriar o banco local.

## APIs principais

| Área | Operações |
|---|---|
| Autenticação | `POST /api/v1/auth/login` |
| Contas | CRUD administrativo em `/api/v1/user-accounts` e perfil em `/me` |
| Clientes | CRUD em `/api/v1/customers`, busca por documento e perfil em `/me` |
| Veículos | CRUD em `/api/v1/vehicles`, busca por placa e veículos do cliente em `/me` |
| Catálogo | CRUD em `/api/v1/service-catalog-items` |
| Estoque | CRUD e reposição, reserva e baixa em `/api/v1/inventory/items` |
| Ordens de serviço | abertura, consulta de status, decisão de orçamento, atualização de status e fila em `/api/v1/service-orders` |

As operações administrativas exigem JWT. Clientes só podem consultar seus próprios dados, veículos e ordens de serviço. As permissões são definidas pelas roles `ADMIN`, `MANAGER`, `SERVICE_ADVISOR`, `TECHNICIAN` e `CUSTOMER`.

## Ordem de serviço

Uma abertura recebe cliente, veículo, serviços e itens de estoque e retorna a identificação da OS. A estimativa é calculada com o preço capturado no momento da abertura. A aprovação ou recusa é idempotente e protege a transição para execução.

Estados: `RECEIVED`, `UNDER_DIAGNOSIS`, `AWAITING_APPROVAL`, `IN_PROGRESS`, `COMPLETED` e `DELIVERED`. A fila operacional prioriza `IN_PROGRESS`, `AWAITING_APPROVAL`, `UNDER_DIAGNOSIS` e `RECEIVED`, da mais antiga para a mais nova, excluindo OS concluídas e entregues.

## Persistência e qualidade

O banco é PostgreSQL 16, escolhido por suas transações, restrições de integridade e índices para os relacionamentos do domínio. O schema é versionado por Flyway em `server/src/main/resources/db/migration`, seguindo `VNN_YYYYMMDD__short_description.sql`.

Auditoria e notificações usam Transactional Outbox: a transação de negócio grava o evento em `event_outbox`, e uma rotina local registra a `audit_trail` imutável e entrega o e-mail depois. Os estados anterior e posterior guardam apenas campos de negócio alterados, sem CPF/CNPJ, telefone ou e-mail.

Para executar os testes com o runtime Java 25 adotado no projeto:

```powershell
docker run --rm -v "${PWD}/server:/workspace" -v "${HOME}/.m2:/root/.m2" -w /workspace maven:3.9-eclipse-temurin-25 mvn test
```

O [relatório de segurança](docs/security/vulnerability-analysis.md) registra o escopo, as evidências e as limitações do scan realizado.
