# JACAFI — FIAP Tech Challenge 01

Back-end monolítico para gestão de uma oficina mecânica, desenvolvido em Java 25 com Spring Boot 4, PostgreSQL e arquitetura Vertical Slice.

## Pré-requisitos

- Docker Desktop com Docker Compose; ou
- Java 25 e Maven 3.9 para executar sem contêineres.

## Execução com Docker

Copie `.env.example` para `.env` e execute:

```bash
docker compose up --build
```

A aplicação fica disponível em `http://localhost:8082`. O Swagger UI pode ser acessado em `http://localhost:8082/swagger-ui.html` e o contrato OpenAPI em `http://localhost:8082/v3/api-docs`.

O PostgreSQL foi escolhido por oferecer integridade relacional, transações ACID, índices e constraints adequados aos relacionamentos entre clientes, veículos, ordens de serviço, serviços e peças. O Flyway versiona o esquema; o Hibernate apenas o valida durante a inicialização.

## Serviço de clientes

O cliente é modelado como um papel associado a uma `Party`. Assim, a mesma pessoa pode futuramente acumular outros papéis, como funcionário ou usuário do sistema, sem duplicar sua identidade fiscal.

| Método | Endpoint | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/clients` | Cadastrar cliente |
| `GET` | `/api/v1/clients/{id}` | Consultar por ID |
| `GET` | `/api/v1/clients/lookup?personType=...&taxIdentifier=...` | Consultar por CPF/CNPJ |
| `GET` | `/api/v1/clients?active=true&page=0&size=20` | Listar clientes |
| `PATCH` | `/api/v1/clients/{id}` | Atualizar nome e contato |
| `DELETE` | `/api/v1/clients/{id}` | Desativar cliente |

CPF e CNPJ são normalizados e validados pelos dígitos verificadores. O CNPJ aceita tanto a composição numérica quanto a alfanumérica. A identidade fiscal e o tipo de pessoa não podem ser alterados depois do cadastro.

Exemplo de cadastro:

```bash
curl -X POST http://localhost:8082/api/v1/clients \
  -H "Content-Type: application/json" \
  -d '{
    "personType": "INDIVIDUAL",
    "taxIdentifier": "529.982.247-25",
    "name": "Maria da Silva",
    "email": "maria@example.com",
    "phone": "11999999999"
  }'
```

As APIs administrativas ficam sujeitas à configuração de segurança/JWT do projeto. Os testes HTTP do recorte de clientes desabilitam apenas os filtros de segurança para validar isoladamente o fluxo funcional.

## Testes e cobertura

Na pasta `server`, execute:

```bash
mvn verify
```

A suíte contém testes unitários e de integração com PostgreSQL via Testcontainers. O build falha se o pacote de domínio de clientes ficar abaixo de 80% de cobertura de linhas.

## Migrações

As migrações ficam em `server/src/main/resources/db/migration`. A versão usa data e sequência, por exemplo:

```text
V20260824_01__create_initial_schema.sql
```

Não altere uma migração já aplicada; crie a próxima sequência para mudanças de esquema.
