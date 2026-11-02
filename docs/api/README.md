# Collection da API e Referência de Endpoints

A collection completa do projeto está versionada em [jacafi.postman_collection.json](jacafi.postman_collection.json) (formato Postman Collection v2.1, compatível também com Insomnia, Bruno e Hoppscotch).

Além da collection, a especificação OpenAPI 3.1 interativa está disponível quando o ambiente está em execução:

| Recurso | Local (Compose) | Local (Kubernetes) | AWS EKS |
|---|---|---|---|
| Swagger UI | `http://localhost:8082/swagger-ui/index.html` | `http://localhost:8082/swagger-ui/index.html` | `http://<alb_dns>/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8082/v3/api-docs` | `http://localhost:8082/v3/api-docs` | `http://<alb_dns>/v3/api-docs` |

---

## Como usar a Collection

1. Importe o arquivo [docs/api/jacafi.postman_collection.json](jacafi.postman_collection.json) no Postman (ou cliente compatível).
2. Confira as variáveis da collection de acordo com o ambiente alvo:

   | Variável | Ambiente Local (padrão) | Ambiente AWS EKS |
   |---|---|---|
   | `baseUrl` | `http://localhost:8082` | `http://<alb_dns>` |
   | `keycloakUrl` | `http://localhost:8081` | `http://<alb_dns>:8080` |
   | `realm` | `jacafi` | `jacafi` |
   | `clientId` | `jacafi-web` | `jacafi-web` |
   | `employeeUsername` / `employeePassword` | `dev-employee` / `Dev-employee-2026` | `dev-employee` / `Dev-employee-2026` |
   | `customerUsername` / `customerPassword` | `dev-customer` / `Dev-customer-2026` | `dev-customer` / `Dev-customer-2026` |

3. Execute as duas requisições da pasta **`00 - Autenticação (Keycloak)`**. Os scripts de pós-resposta salvam automaticamente `employeeToken` e `customerToken` nas variáveis da collection (validade de 15 minutos).
4. Execute as pastas de `01` a `09` em sequência. As requisições de criação geram CPF e placa válidos aleatórios e salvam automaticamente `customerId`, `vehicleId`, `laborOperationId`, `inventoryItemId`, `serviceOrderId` e `estimateId` nas variáveis da collection para alimentar os passos seguintes.

---

## Catálogo de Endpoints

Todas as rotas sob `/api/v1/**` exigem autenticação via Bearer JWT emitido pelo Keycloak.

- **`EMPLOYEE`**: acesso completo às operações administrativas e operacionais da oficina.
- **`CUSTOMER`**: acesso restrito aos próprios dados (`/customers/me`, `/vehicles/me`), consulta de status da própria OS (`GET /service-orders/{id}/status`) e aprovação/reprovação do orçamento da própria OS.

### 01. Saúde e Observabilidade

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| `GET` | `/actuator/health/liveness` | Público | Probe de liveness da aplicação |
| `GET` | `/actuator/health/readiness` | Público | Probe de readiness (verifica conectividade com o PostgreSQL) |

### 02. Clientes (`/api/v1/customers`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/customers` | `EMPLOYEE` | Cadastra cliente (valida dígitos verificadores de CPF/CNPJ) |
| `GET` | `/api/v1/customers/{id}` | `EMPLOYEE` | Busca cliente por identificador |
| `GET` | `/api/v1/customers/lookup?taxId={cpfCnpj}` | `EMPLOYEE` | Busca cliente por CPF ou CNPJ (aceita com ou sem máscara) |
| `GET` | `/api/v1/customers?active={bool}&page=0&size=20&sort=name,asc` | `EMPLOYEE` | Lista clientes com paginação e filtro opcional de ativo |
| `PATCH` | `/api/v1/customers/{id}` | `EMPLOYEE` | Atualiza nome, nome fantasia, e-mail e telefone do cliente |
| `DELETE` | `/api/v1/customers/{id}` | `EMPLOYEE` | Desativa o cadastro do cliente (exclusão lógica) |
| `PUT` | `/api/v1/customers/{id}/identity` | `EMPLOYEE` | Vincula o `sub` do usuário no Keycloak ao cadastro do cliente |
| `GET` | `/api/v1/customers/me` | `CUSTOMER` | Retorna os dados cadastrais do cliente autenticado |
| `PATCH` | `/api/v1/customers/me` | `CUSTOMER` | Atualiza os dados cadastrais do próprio cliente autenticado |

### 03. Veículos (`/api/v1/vehicles`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/vehicles` | `EMPLOYEE` | Cadastra veículo vinculado a um cliente (placa antiga ou Mercosul) |
| `GET` | `/api/v1/vehicles/{vehicleId}` | `EMPLOYEE` | Busca veículo por identificador |
| `GET` | `/api/v1/vehicles/lookup?licensePlate={placa}` | `EMPLOYEE` | Busca veículo pela placa |
| `GET` | `/api/v1/vehicles?customerId={id}&page=0&size=20` | `EMPLOYEE` | Lista veículos de um cliente específico |
| `PUT` | `/api/v1/vehicles/{vehicleId}` | `EMPLOYEE` | Atualiza marca, modelo e ano do veículo |
| `DELETE` | `/api/v1/vehicles/{vehicleId}` | `EMPLOYEE` | Remove veículo (exclusão lógica) |
| `GET` | `/api/v1/vehicles/me?page=0&size=20` | `CUSTOMER` | Lista os veículos pertencentes ao cliente autenticado |

### 04. Catálogo de Serviços (`/api/v1/labor-operations`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/labor-operations` | `EMPLOYEE` | Cadastra serviço no catálogo com preço base |
| `GET` | `/api/v1/labor-operations/{id}` | `EMPLOYEE` | Busca serviço do catálogo por identificador |
| `GET` | `/api/v1/labor-operations?page=0&size=20` | `EMPLOYEE` | Lista serviços ativos do catálogo |
| `PUT` | `/api/v1/labor-operations/{id}` | `EMPLOYEE` | Atualiza nome, descrição e preço base do serviço |
| `DELETE` | `/api/v1/labor-operations/{id}` | `EMPLOYEE` | Desativa serviço do catálogo |

### 05. Estoque (`/api/v1/inventory/items`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/inventory/items` | `EMPLOYEE` | Cadastra peça (`PART`) ou insumo (`SUPPLY`) com estoque inicial |
| `GET` | `/api/v1/inventory/items/{id}` | `EMPLOYEE` | Consulta item, saldos (`stockOnHand`, `stockReserved`, `stockAvailable`) e reservas |
| `GET` | `/api/v1/inventory/items?type={PART\|SUPPLY}&page=0&size=20` | `EMPLOYEE` | Lista itens do estoque com filtro opcional por tipo |
| `PUT` | `/api/v1/inventory/items/{id}` | `EMPLOYEE` | Atualiza nome e preço unitário do item |
| `DELETE` | `/api/v1/inventory/items/{id}` | `EMPLOYEE` | Remove item do estoque (exige não possuir reservas em aberto) |
| `POST` | `/api/v1/inventory/items/{id}/replenishments` | `EMPLOYEE` | Repõe unidades no estoque físico |
| `POST` | `/api/v1/inventory/items/{id}/reservations` | `EMPLOYEE` | Reserva unidades do item para uma Ordem de Serviço |
| `DELETE` | `/api/v1/inventory/items/{id}/reservations/{serviceOrderId}` | `EMPLOYEE` | Libera reserva ativa de uma Ordem de Serviço |
| `POST` | `/api/v1/inventory/items/{id}/withdrawals` | `EMPLOYEE` | Dá baixa definitiva na reserva vinculada à Ordem de Serviço |

### 06. Ordens de Serviço (`/api/v1/service-orders`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/service-orders` | `EMPLOYEE` | Abre OS com serviços e materiais, reserva itens no estoque, calcula o orçamento e move para `AWAITING_APPROVAL` |
| `GET` | `/api/v1/service-orders/{serviceOrderId}/status` | `EMPLOYEE` / `CUSTOMER` (dono) | Consulta o status atual da OS e a lista de orçamentos com valores e datas |
| `POST` | `/api/v1/service-orders/{serviceOrderId}/estimates/{estimateId}/approval` | `EMPLOYEE` / `CUSTOMER` (dono) | Aprova o orçamento de forma idempotente (`idempotencyKey`) e avança a OS para `IN_PROGRESS` |
| `POST` | `/api/v1/service-orders/{serviceOrderId}/estimates/{estimateId}/rejection` | `EMPLOYEE` / `CUSTOMER` (dono) | Reprova o orçamento de forma idempotente (`idempotencyKey`) e retorna a OS para `UNDER_DIAGNOSIS` |
| `PATCH` | `/api/v1/service-orders/{serviceOrderId}/status` | `EMPLOYEE` | Atualiza o status da OS (`UNDER_DIAGNOSIS`, `COMPLETED` ou `DELIVERED`) respeitando a máquina de estados |
| `GET` | `/api/v1/service-orders?page=0&size=20` | `EMPLOYEE` | Lista a fila operacional ordenada por prioridade (`IN_PROGRESS`, `AWAITING_APPROVAL`, `UNDER_DIAGNOSIS`, `RECEIVED`) e antiguidade, excluindo `COMPLETED` e `DELIVERED` |

