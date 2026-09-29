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

Fatias atuais: `customer`, `vehicle`, `inventory`, `servicecatalog` e `serviceorder`. Itens compartilhados são limitados a preocupações transversais, como segurança, auditoria, erros, paginação e tempo. A autenticação não é uma fatia: fica no Keycloak, e a aplicação apenas valida o token (ver [ADR-005](docs/adr/ADR-005-keycloak-identity-provider.md)).

As decisões arquiteturais estão em [docs/adr](docs/adr), os termos do domínio em [docs/linguagem-ubiqua.md](docs/linguagem-ubiqua.md), os fluxos em [docs/event-storming.md](docs/event-storming.md) e as visões de arquitetura em [docs/c4-model.md](docs/c4-model.md).

## Execução local

Pré-requisitos: Docker e Docker Compose. Para executar fora do container, use Java 25 e Maven 3.9+.

```powershell
docker compose --env-file .env.example up -d --build
```

O Compose sobe PostgreSQL, Keycloak e a API. O Keycloak importa o realm versionado em [keycloak/jacafi-realm.json](keycloak/jacafi-realm.json) na subida; o banco dele não é persistido, então o arquivo é a única fonte da verdade do realm local. Os serviços ficam disponíveis em:

| Recurso | Endereço |
|---|---|
| API | `http://localhost:8082` |
| Keycloak | `http://localhost:8081` |
| Swagger UI | `http://localhost:8082/swagger-ui/index.html` |
| OpenAPI | `http://localhost:8082/v3/api-docs` |
| Liveness | `http://localhost:8082/actuator/health/liveness` |
| Readiness | `http://localhost:8082/actuator/health/readiness` |

`APP_PORT`, `DATABASE_PORT` e `KEYCLOAK_PORT` permitem alterar somente as portas expostas no host; `SERVER_PORT` mantém a porta interna da aplicação. O Compose aguarda a saúde do PostgreSQL e do Keycloak antes de iniciar a API e verifica a prontidão da aplicação.

`KEYCLOAK_ISSUER_URI` é o endereço público do realm, usado para validar o claim `iss` do token; `KEYCLOAK_JWK_SET_URI` é o endereço interno da rede do Compose, usado para obter as chaves de assinatura. Os dois são distintos de propósito, porque o navegador e a API alcançam o Keycloak por endereços diferentes.

## Autenticação

O Keycloak emite os tokens; a aplicação é apenas um *resource server* e não guarda senha nem lista de usuários. O realm de desenvolvimento traz o client público `jacafi-web` e dois usuários:

| Usuário | Senha | Role |
|---|---|---|
| `dev-employee` | `Dev-employee-2026` | `employee` |
| `dev-customer` | `Dev-customer-2026` | `customer` |

Para obter um token no ambiente local:

```bash
curl -s -X POST http://localhost:8081/realms/jacafi/protocol/openid-connect/token \
  -d client_id=jacafi-web -d grant_type=password \
  -d username=dev-employee -d password=Dev-employee-2026
```

Um cliente só alcança os próprios dados depois que um funcionário vincula a identidade do Keycloak ao cadastro, com o `sub` do token:

```bash
curl -X PUT http://localhost:8082/api/v1/customers/{customerId}/identity \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"subjectId":"f0000000-0000-0000-0000-000000000002"}'
```

A gestão de usuários, roles, senhas e sessões é feita no console do Keycloak, em `http://localhost:8081` com as credenciais de `KEYCLOAK_ADMIN` e `KEYCLOAK_ADMIN_PASSWORD`. A aplicação decide apenas o que cada role alcança.

As credenciais do realm versionado e do console existem apenas para o ambiente local. Em outro ambiente, importe um realm próprio e defina as senhas por variável de ambiente.

A imagem de runtime desabilita OpenAPI e Swagger por padrão. O Compose os habilita somente para desenvolvimento local; em outro ambiente, mantenha `SPRINGDOC_API_DOCS_ENABLED=false` e `SPRINGDOC_SWAGGER_UI_ENABLED=false`, ou proteja os endpoints por rede e autenticação.

## Credenciais de demonstração

O banco criado do zero inclui uma conta administrativa apenas para demonstração:

| Usuário | Senha | Role |
|---|---|---|
| `dev-admin` | `admin123` | `ADMIN` |

Para aplicar essa credencial após uma execução anterior, recrie o banco local antes de subir o Compose:

```powershell
docker compose down --volumes
docker compose up --build
```

Não use essa senha fora do ambiente local de demonstração.

Para habilitar os e-mails de mudança de status da OS, preencha somente o seu `.env` local com `RESEND_ENABLED=true`, `RESEND_API_KEY` e `RESEND_FROM`. A chave não é versionada nem registrada em logs. O remetente deve pertencer a um domínio verificado no Resend; mantenha o recurso desabilitado quando não houver credenciais.

Para encerrar o ambiente:

```powershell
docker compose down
```

Use `docker compose down --volumes` apenas quando for necessário recriar o banco local.

## APIs principais

| Área | Operações |
|---|---|
| Clientes | CRUD em `/api/v1/customers`, busca por documento, vínculo de identidade em `/{id}/identity` e perfil em `/me` |
| Veículos | CRUD em `/api/v1/vehicles`, busca por placa e veículos do cliente em `/me` |
| Catálogo | CRUD em `/api/v1/service-catalog-items` |
| Estoque | CRUD e reposição, reserva e baixa em `/api/v1/inventory/items` |
| Ordens de serviço | abertura, consulta de status, decisão de orçamento, atualização de status e fila em `/api/v1/service-orders` |

Toda operação exige JWT. As permissões são definidas por duas roles: `EMPLOYEE` alcança todos os recursos da oficina; `CUSTOMER` só alcança os próprios dados, veículos e ordens de serviço, sempre pelos recursos `/me` e pela consulta da própria OS.

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

A cobertura é medida com JaCoCo na fase `verify`; o processo e o limite mínimo estão em [docs/cobertura-testes.md](docs/cobertura-testes.md).

O [relatório de segurança](docs/security/vulnerability-analysis.md) registra o escopo, as evidências e as limitações do scan realizado.
