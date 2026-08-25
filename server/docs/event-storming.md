# Event Storming — SINATES

Notação conforme BRANDOLINI, A. *Introducing EventStorming*. Nível 3 (*Software Design*): as raias representam **agregados**, não módulos.

**Nota de método:** Event Storming é formato de workshop. Este quadro foi elaborado pelo grupo a partir do enunciado, sem sessão com especialista do domínio. Os pontos quentes marcam onde falta essa validação.

## Legenda

| Cor | Elemento | Convenção de nome |
|---|---|---|
| 🟨 Amarelo claro | Ator | substantivo |
| 🟦 Azul | Comando | verbo no imperativo |
| 🟧 Laranja | Evento de domínio | particípio passado |
| 🟪 Roxo | Política | "sempre que… então…" |
| 🟩 Verde | Read model | substantivo |
| 🟥 Vermelho | Ponto quente | pergunta em aberto |

Mudanças de status **não são comandos nem eventos** — são estado interno do agregado, anotadas junto ao evento que as provoca.

---

## Fluxo principal — abertura e diagnóstico

```mermaid
flowchart TD
    classDef actor fill:#FFE699,stroke:#D6B656,color:#000
    classDef command fill:#7EA6E0,stroke:#3D6AA2,color:#000
    classDef event fill:#FFA366,stroke:#D79B00,color:#000
    classDef policy fill:#E1D5E7,stroke:#9673A6,color:#000
    classDef readmodel fill:#97D077,stroke:#5D8C3F,color:#000
    classDef hotspot fill:#EA6B66,stroke:#B85450,color:#000

    ACT1(["Service Advisor"]):::actor
    RM1[/"Customer List"/]:::readmodel
    CMD1["FindCustomer"]:::command
    EVT1["CustomerIdentified"]:::event
    EVT2["CustomerNotFound"]:::event
    CMD2["RegisterCustomer"]:::command
    EVT3["CustomerRegistered"]:::event

    RM2[/"Vehicle List"/]:::readmodel
    CMD3["FindVehicle"]:::command
    EVT4["VehicleIdentified"]:::event
    EVT5["VehicleNotFound"]:::event
    CMD4["RegisterVehicle"]:::command
    EVT6["VehicleRegistered"]:::event

    CMD5["OpenServiceOrder"]:::command
    EVT7["ServiceOrderOpened<br/><i>status → RECEIVED</i>"]:::event

    ACT2(["Technician"]):::actor
    CMD6["StartDiagnosis"]:::command
    EVT8["DiagnosisStarted<br/><i>status → UNDER_DIAGNOSIS</i>"]:::event
    CMD7["RecordDiagnosis"]:::command
    EVT9["DiagnosisRecorded"]:::event

    ACT1 --> CMD1
    RM1 -.-> CMD1
    CMD1 --> EVT1
    CMD1 -.-> EVT2
    EVT2 -.-> CMD2
    CMD2 --> EVT3
    EVT1 --> CMD3
    EVT3 --> CMD3
    RM2 -.-> CMD3
    CMD3 --> EVT4
    CMD3 -.-> EVT5
    EVT5 -.-> CMD4
    CMD4 --> EVT6
    EVT4 --> CMD5
    EVT6 --> CMD5
    CMD5 --> EVT7
    ACT2 --> CMD6
    EVT7 --> CMD6
    CMD6 --> EVT8
    EVT8 --> CMD7
    ACT2 --> CMD7
    CMD7 --> EVT9
```

`CustomerNotFound` e `VehicleNotFound` estão desenhados como eventos por fidelidade ao quadro original do grupo, mas **não são eventos de domínio** — nada muda no mundo quando uma busca não encontra. No nível de código são retorno vazio de read model.

---

## Núcleo — orçamento e portão da aprovação

```mermaid
flowchart TD
    classDef actor fill:#FFE699,stroke:#D6B656,color:#000
    classDef command fill:#7EA6E0,stroke:#3D6AA2,color:#000
    classDef event fill:#FFA366,stroke:#D79B00,color:#000
    classDef policy fill:#E1D5E7,stroke:#9673A6,color:#000
    classDef hotspot fill:#EA6B66,stroke:#B85450,color:#000

    EVT9["DiagnosisRecorded"]:::event
    POL1["WhenDiagnosisRecorded<br/>calculate the estimate"]:::policy
    CMD8["CalculateEstimate"]:::command
    EVT10["EstimateCalculated"]:::event

    POL2["WhenEstimateCalculated<br/>send it to the customer"]:::policy
    CMD9["SendEstimate"]:::command
    EVT11["EstimateSent<br/><i>status → AWAITING_APPROVAL</i>"]:::event
    HS1{{"How is the estimate<br/>delivered to the customer?"}}:::hotspot

    ACT3(["Customer"]):::actor
    CMD10["ApproveEstimate"]:::command
    EVT12["EstimateApproved<br/><i>status → IN_PROGRESS</i>"]:::event
    CMD11["RejectEstimate"]:::command
    EVT13["EstimateRejected<br/><i>status → REJECTED</i>"]:::event

    POL3["WhenEstimateApproved<br/>reserve materials"]:::policy
    POL4["WhenEstimateRejected<br/>release reservations"]:::policy
    POL5["WhenApprovalDeadlineExpires<br/>expire the estimate"]:::policy
    EVT14["EstimateExpired<br/><i>status → REJECTED</i>"]:::event
    HS2{{"How long until<br/>the estimate expires?"}}:::hotspot

    EVT9 --> POL1
    POL1 --> CMD8
    CMD8 --> EVT10
    EVT10 --> POL2
    POL2 --> CMD9
    CMD9 --> EVT11
    HS1 -.-> CMD9
    EVT11 --> CMD10
    EVT11 --> CMD11
    ACT3 --> CMD10
    ACT3 --> CMD11
    CMD10 --> EVT12
    CMD11 --> EVT13
    EVT12 --> POL3
    EVT13 --> POL4
    EVT11 --> POL5
    POL5 --> EVT14
    HS2 -.-> POL5
    EVT14 --> POL4
```

`CalculateEstimate` e `SendEstimate` aparecem como comandos disparados por política, não por ator. O enunciado diz que o orçamento é *"gerado automaticamente"* — logo ninguém o dispara à mão.

---

## Execução, entrega e reparo adicional

```mermaid
flowchart TD
    classDef actor fill:#FFE699,stroke:#D6B656,color:#000
    classDef command fill:#7EA6E0,stroke:#3D6AA2,color:#000
    classDef event fill:#FFA366,stroke:#D79B00,color:#000
    classDef policy fill:#E1D5E7,stroke:#9673A6,color:#000
    classDef hotspot fill:#EA6B66,stroke:#B85450,color:#000

    EVT12["EstimateApproved<br/><i>status → IN_PROGRESS</i>"]:::event
    ACT2(["Technician"]):::actor

    CMD12["AddAdditionalRepair"]:::command
    EVT15["AdditionalRepairAdded<br/><i>status → AWAITING_APPROVAL</i>"]:::event
    HS3{{"What happens to a<br/>disassembled vehicle if the<br/>supplementary estimate<br/>is rejected?"}}:::hotspot

    CMD13["CompleteServices"]:::command
    EVT16["ServicesCompleted<br/><i>status → COMPLETED</i>"]:::event
    POL6["WhenServicesCompleted<br/>withdraw reserved materials"]:::policy
    POL7["WhenServicesCompleted<br/>notify the customer"]:::policy
    EVT17["CustomerNotified"]:::event
    HS4{{"Through which channel<br/>is the customer notified?"}}:::hotspot

    ACT1(["Service Advisor"]):::actor
    CMD14["DeliverVehicle"]:::command
    EVT18["VehicleDelivered<br/><i>status → DELIVERED</i>"]:::event
    HS5{{"Is payment required<br/>before delivery?"}}:::hotspot

    EVT12 --> CMD13
    EVT12 -.-> CMD12
    ACT2 --> CMD12
    ACT2 --> CMD13
    CMD12 --> EVT15
    EVT15 -.-> HS3
    CMD13 --> EVT16
    EVT16 --> POL6
    EVT16 --> POL7
    POL7 --> EVT17
    HS4 -.-> POL7
    EVT17 --> CMD14
    ACT1 --> CMD14
    CMD14 --> EVT18
    HS5 -.-> CMD14
```

`AddAdditionalRepair` é o **único retrocesso do quadro**: a OS volta de `IN_PROGRESS` para `AWAITING_APPROVAL`. Todo o restante é linear.

---

## Agregado Inventory

```mermaid
flowchart TD
    classDef actor fill:#FFE699,stroke:#D6B656,color:#000
    classDef command fill:#7EA6E0,stroke:#3D6AA2,color:#000
    classDef event fill:#FFA366,stroke:#D79B00,color:#000
    classDef policy fill:#E1D5E7,stroke:#9673A6,color:#000

    POL3["WhenEstimateApproved"]:::policy
    CMD15["ReserveMaterial"]:::command
    EVT19["MaterialReserved"]:::event

    POL4["WhenEstimateRejected"]:::policy
    CMD16["ReleaseReservation"]:::command
    EVT20["ReservationReleased"]:::event

    POL6["WhenServicesCompleted"]:::policy
    CMD17["WithdrawMaterial"]:::command
    EVT21["MaterialWithdrawn"]:::event

    ACT4(["Manager"]):::actor
    CMD18["RegisterMaterial"]:::command
    EVT22["MaterialRegistered"]:::event
    CMD19["ReplenishStock"]:::command
    EVT23["StockReplenished"]:::event

    POL3 --> CMD15
    CMD15 --> EVT19
    POL4 --> CMD16
    CMD16 --> EVT20
    POL6 --> CMD17
    CMD17 --> EVT21
    ACT4 --> CMD18
    CMD18 --> EVT22
    ACT4 --> CMD19
    CMD19 --> EVT23
```

`Inventory` é agregado separado de `ServiceOrder` porque a consistência entre eles tolera atraso: se a reserva sair segundos depois da aprovação, o pior caso é uma peça a menos disponível por segundos. A ligação é feita por política, nunca por chamada direta.

---

## Agregados e fronteiras

| Agregado | Contém | Justificativa da fronteira |
|---|---|---|
| `ServiceOrder` | Diagnosis, Estimate, ServiceLineItem, MaterialLineItem, StatusHistory | A invariante "nenhum serviço executado sem aprovação registrada" não tolera janela de inconsistência |
| `Customer` | TaxId | Ciclo de vida próprio, existe sem OS |
| `Vehicle` | LicensePlate | Ciclo de vida próprio, sobrevive a múltiplas OS e a troca de dono |
| `Inventory` | InventoryItem, Reservation | Consistência eventual aceitável com `ServiceOrder` |

`Estimate` é entidade **interna** a `ServiceOrder`, não raiz própria. Migra para agregado próprio se surgir cotação sem OS associada, se a contenção entre aprovação e lançamento se tornar mensurável, ou se a retenção do orçamento passar a exceder a da OS.

---

## Pontos quentes

| # | Pergunta | Impacto |
|---|---|---|
| HS1 | Por qual canal o orçamento chega ao cliente? | Define se há integração externa |
| HS2 | Qual o prazo até o orçamento expirar? | Reserva sem prazo trava estoque indefinidamente |
| HS3 | O que acontece com veículo desmontado se o complementar for recusado? | Sem resposta, o retrocesso do fluxo fica indefinido |
| HS4 | Por qual canal o cliente é notificado da conclusão? | Idem HS1 |
| HS5 | Há pagamento antes da entrega? | O enunciado não menciona dinheiro trocando de mãos |
| HS6 | "Aprovação" e "autorização" são o mesmo ato? | O cliente citou os dois separadamente na descrição da dor |
| HS7 | A remoção de veículo atende a dois atos com base legal distinta? | A base legal muda o que precisa ser provado depois, e a trilha atual não distingue |

**HS7 em detalhe.** `RemoveVehicle` cobre hoje duas situações que só se parecem na superfície. Na primeira, a oficina limpa o cadastro de um veículo que não atende mais: decisão administrativa dela, sem titular envolvido. Na segunda, o titular exerce o direito de eliminação do dado pessoal (LGPD Art. 18 VI): pedido dele, com prazo de resposta e dever de comprovação por parte da oficina.

As duas terminam na mesma anonimização, e é isso que torna aceitável tratá-las como um caso de uso único no MVP. Mas a base legal é diferente, e a trilha de auditoria registra apenas que o veículo foi removido — não a pedido de quem, nem sob qual fundamento. Se o titular questionar o atendimento do pedido, o registro atual não serve de prova. Separar os dois atos, quando for a hora, significa dois comandos distintos e um campo de fundamento na trilha, não um `boolean`.
