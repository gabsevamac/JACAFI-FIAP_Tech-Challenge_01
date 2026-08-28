# Linguagem ubíqua

| Português | Código em inglês | Definição |
|---|---|---|
| Cliente | `Customer` | Pessoa física ou jurídica atendida pela oficina. |
| Conta de usuário | `UserAccount` | Credencial de acesso, que pode opcionalmente estar vinculada a um cliente. |
| Papel | `Role` | Nível de acesso da conta: `ADMIN`, `MANAGER`, `SERVICE_ADVISOR`, `TECHNICIAN` ou `CUSTOMER`. |
| Documento fiscal | `TaxIdentifier` | CPF ou CNPJ que identifica um cliente. |
| Veículo | `Vehicle` | Bem atendido pela oficina e associado a um cliente. |
| Placa | `LicensePlate` | Identificador de busca do veículo, validado nos formatos brasileiro e Mercosul. |
| Item de estoque | `InventoryItem` | Peça ou insumo controlado por quantidade. |
| Serviço de catálogo | `ServiceCatalogItem` | Serviço oferecido pela oficina com preço-base. |
| Ordem de serviço | `ServiceOrder` | Registro do atendimento de um veículo, do recebimento à entrega. |
| Orçamento | `Estimate` | Valor apresentado ao cliente, calculado a partir dos itens da OS. |
| Decisão de orçamento | `EstimateDecision` | Aprovação ou recusa registrada de forma idempotente. |

## Regras de linguagem

- Uma conta não é necessariamente um cliente: funcionários podem possuir uma conta e também tornar-se clientes.
- CPF e CNPJ são campos distintos, com índices próprios. A busca valida o documento antes de consultar o campo correspondente.
- CNPJ admite os formatos numérico e alfanumérico previstos para sua vigência. A validação usa os dígitos verificadores, não apenas tamanho ou expressão regular.
- Um orçamento captura os preços no momento da abertura; ajustes posteriores do catálogo ou estoque não alteram uma estimativa existente.
- Uma OS pode estar em `RECEIVED`, `UNDER_DIAGNOSIS`, `AWAITING_APPROVAL`, `IN_PROGRESS`, `COMPLETED` ou `DELIVERED`.
- A execução só começa após a aprovação do orçamento. Conclusão e entrega obedecem a transições explícitas do agregado.
