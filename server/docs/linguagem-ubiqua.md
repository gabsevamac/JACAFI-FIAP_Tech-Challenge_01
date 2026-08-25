# Dicionário da Linguagem Ubíqua — SINATES

**Sistema Integrado de Atendimento e Execução de Serviços**
Versão 2 — estrutura derivada de Evans, E. *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley, 2003.

---

## Nota metodológica

Evans não prescreve um formato de glossário. No cap. 2 ele trata a Ubiquitous Language como prática — algo que vive simultaneamente na fala, no modelo e no código — e adverte que um documento não deve duplicar o que o código já expressa bem. A macroestrutura abaixo foi portanto **derivada dos padrões do livro**, com a correspondência declarada:

| Seção deste documento | Padrão / capítulo |
|---|---|
| §1 Declaração de visão do domínio | *Domain Vision Statement* (cap. 15) |
| §2 Contexto delimitado | *Bounded Context* (cap. 14) |
| §3 Destilação | *Core Domain*, *Generic Subdomain* (cap. 15) |
| §4 Núcleo — verbetes | *Highlighted Core* (cap. 15); blocos de construção (caps. 5–6) |
| §5 Subdomínios de apoio e genéricos — verbetes | *Generic Subdomain* (cap. 15), estendido por Vernon (2013) e Khononov (2021) |
| §6 Conceitos implícitos tornados explícitos | *Making Implicit Concepts Explicit* (cap. 9) |
| §7 Linguagem descartada | Escrutínio do desconforto na linguagem (caps. 2 e 9) |
| §8 Questões em aberto | *Crunching Knowledge* (cap. 1) — modelagem como processo contínuo |
| §9 Índice alfabético | — |

**Origem dos termos:** extração de substantivos e verbos do enunciado pela técnica de Abbott (Abbott, R. J. *Program Design by Informal English Descriptions*. Communications of the ACM 26(11), 1983), refinada segundo os critérios do cap. 9 de Evans.

**Microestrutura do verbete:** Evans não a especifica. Adotou-se a ISO 10241-1:2011 (termo preferido, termo depreciado, definição, exemplo, nota, fonte), que é a norma para redação e estruturação de verbetes terminológicos.

---

## §1 Declaração de visão do domínio

> Uma oficina mecânica de médio porte executa manutenção em veículos de terceiros. Cada intervenção envolve dinheiro que não é da oficina e um bem que não é da oficina — o que torna a **autorização do cliente** a condição de legitimidade de todo trabalho executado.
>
> Hoje essa autorização circula por anotações e planilhas, sem rastro. O SINATES existe para tornar o ciclo diagnóstico → orçamento → aprovação → execução **auditável e visível ao cliente em tempo real**, de modo que nenhum serviço seja executado sem consentimento registrado e nenhuma peça saia do estoque sem vínculo com uma ordem aprovada.

Tudo que não serve a essa frase é subdomínio genérico.

---

## §2 Contexto delimitado

**Nome:** Gestão de Ordens de Serviço
**Extensão:** único contexto do MVP (Minimum Viable Product). Back-end monolítico em camadas.

Este dicionário **só vale dentro deste contexto**. A advertência não é formal: `Serviço` aqui significa trabalho executado em veículo; num futuro contexto de faturamento significaria linha tributável; num contexto de infraestrutura significaria processo em execução. São conceitos distintos que compartilham a palavra.

**Fronteiras conhecidas:** não pertencem a este contexto — faturamento fiscal, folha de pagamento, compras/fornecedores, agendamento de horários.

---

## §3 Destilação

### Núcleo (Core Domain)

O ciclo de vida da Ordem de Serviço com a aprovação funcionando como portão. É o que a oficina está comprando e onde os melhores esforços de modelagem devem ir.

Pertencem ao núcleo: `Ordem de Serviço` · `Status` · `Transição` · `Regra de Transição` · `Histórico de Status` · `Diagnóstico` · `Orçamento` · `Aprovação` · `Reprovação` · `Reparo Adicional` · `Orçamento Complementar` · `Serviço Lançado` · `Item Lançado`

### Subdomínios de apoio (Supporting)

Precisam ser construídos sob medida — não há solução de prateleira que respeite as regras desta oficina — mas nenhum cliente escolhe a oficina por causa deles.

Pertencem ao apoio: `Serviço` (catálogo) · `Peça` · `Insumo` · `Item de Estoque` · `Estoque` · `Baixa`

`Estoque` cai aqui, e não em genérico, porque a baixa precisa estar vinculada a uma ordem aprovada — invariante que nenhum módulo de prateleira conhece. `Serviço` (catálogo) idem: é a tabela de preços daquela oficina.

### Subdomínios genéricos (Generic)

Sem diferencial competitivo **e** disponíveis prontos. Construir à mão é desperdício.

Pertencem aos genéricos: `Cliente` · `Veículo` · `Placa` · `Identificação`

`Identificação` e `Placa` são validações de formato com biblioteca disponível. `Cliente` e `Veículo` são cadastro elementar, sem regra própria neste MVP — se ganharem regra (frota, histórico de propriedade), migram para apoio.

### Critério aplicado

| Categoria | Diferencia o negócio? | Existe pronto? |
|---|---|---|
| Principal | sim | não |
| Apoio | não | não |
| Genérico | não | sim |

**Nota sobre a taxonomia:** Evans, no cap. 15, trabalha com **duas** categorias — *Core Domain* e *Generic Subdomain*. A categoria intermediária *Supporting Subdomain* é extensão posterior, consolidada por Vernon (2013, cap. 2) e sistematizada por Khononov (2021). Adotou-se a divisão tripartite por precisão descritiva: classificar `Estoque` como genérico carregaria a recomendação implícita de comprá-lo pronto, o que é falso neste domínio. O afastamento em relação à âncora fica declarado.

---

## §4 Núcleo — verbetes

### Ordem de Serviço
**Estereótipo:** Entidade · raiz de agregado (cap. 6)
**Definição:** Registro do conjunto de trabalhos que a oficina executará em um veículo, para um cliente, numa visita — desde o recebimento até a entrega.
**Sigla admitida:** OS
**Termos depreciados:** chamado, ticket, pedido
**Exemplo:** "Abre uma OS pro Gol prata que acabou de chegar."
**Nota:** É a raiz do agregado que protege a invariante central — nenhum serviço executado sem aprovação registrada.
**Fonte:** "Criação da Ordem de Serviço (OS)"

### Status
**Estereótipo:** Objeto de Valor (cap. 5)
**Definição:** Situação em que uma ordem de serviço se encontra. Uma OS possui exatamente um status por vez.
**Extensão:** Recebida · Em diagnóstico · Aguardando aprovação · Em execução · Finalizada · Entregue
**Termos depreciados:** etapa, fase, situação, processo
**Nota:** Os quatro "processos" da introdução do enunciado — atendimento, diagnóstico, execução de serviços, entrega — são este mesmo ciclo visto pela oficina em vez de pela OS. Manter os dois vocabulários criaria sinonímia interna, que é o defeito que a Ubiquitous Language existe para eliminar (cap. 2).
**Fonte:** "Status da OS"

### Transição
**Estereótipo:** Objeto de Valor · candidato a Evento de Domínio
**Definição:** Passagem de uma ordem de serviço de um status para outro, provocada por uma ação identificável, registrando origem, destino, instante e responsável.
**Exemplo:** "A transição pra execução saiu 14h20, depois que o cliente aprovou."
**Nota:** Ver §6 — conceito implícito promovido.
**Fonte:** "Alteração automática dos status conforme ações no sistema"

### Regra de Transição
**Estereótipo:** Especificação (*Specification*, cap. 9)
**Definição:** Condição que determina se uma transição específica é permitida no estado atual da ordem de serviço.
**Exemplo:** "A regra não deixa ir pra execução sem aprovação."
**Nota:** Evans trata restrições explícitas como conceitos de primeira classe no cap. 9. É aqui que as invariantes do agregado se tornam nomeáveis, e é este conjunto de regras que define a fronteira do agregado — a Aprovação precisa estar dentro da OS porque a restrição não admite janela de inconsistência.
**Fonte:** derivado; ver §6

### Histórico de Status
**Estereótipo:** parte interna do agregado Ordem de Serviço
**Definição:** Sequência ordenada das transições já ocorridas em uma ordem de serviço.
**Nota:** Sem ele o indicador de tempo médio de execução é incalculável — o status corrente informa onde a OS está, não quanto tempo levou para chegar. Relação com o campo `Status`: o mesmo que extrato para saldo.
**Fonte:** derivado de "Monitoramento do tempo médio de execução dos serviços"

### Diagnóstico
**Estereótipo:** Entidade interna ao agregado
**Definição:** Constatação registrada do mecânico sobre o que o veículo necessita, da qual derivam os serviços e peças a orçar.
**Não confundir com:** o status *Em diagnóstico*, que é o intervalo durante o qual o diagnóstico é elaborado.
**Exemplo:** "O diagnóstico apontou correia e tensor."
**Nota:** É o que justifica o orçamento perante o cliente. No enunciado aparece apenas como etapa; promovê-lo a artefato registrado é o que dá rastreabilidade à cobrança.
**Fonte:** "processo de atendimento, diagnóstico, execução de serviços"

### Orçamento
**Estereótipo:** Entidade interna ao agregado
**Definição:** Valor que a oficina apresenta ao cliente antes de executar o trabalho, somando os serviços e itens lançados na ordem de serviço.
**Exemplo:** "Manda o orçamento pra ela ver."
**Nota:** Gerado automaticamente a partir dos lançamentos, mas não é um mero cálculo — sai da oficina, chega ao cliente e recebe resposta, o que lhe confere identidade e ciclo próprios.
**Fonte:** "Orçamento gerado automaticamente com base nos serviços e peças"

### Aprovação
**Estereótipo:** Objeto de Valor · candidato a Evento de Domínio
**Definição:** Manifestação do cliente concordando com um orçamento, que autoriza a oficina a executar o trabalho nele descrito.
**Termo depreciado:** **autorização**
**Exemplo:** "Saiu a aprovação, pode subir no elevador."
**Nota:** O enunciado usa as duas palavras para o mesmo ato — "para aprovação" no fluxo do orçamento, "autorizar reparos adicionais" no aplicativo. Sinonímia eliminada em favor de *aprovação*, também porque *autorização* colide com o vocabulário de controle de acesso do próprio sistema.
**Fonte:** "Envio do orçamento ao cliente para aprovação"

### Reprovação
**Estereótipo:** Objeto de Valor · candidato a Evento de Domínio
**Definição:** Manifestação do cliente recusando um orçamento.
**Nota:** Não há status previsto para o desfecho desta ação. Ver Q1.

### Reparo Adicional
**Estereótipo:** conceito do domínio, materializado como Serviço Lançado posterior
**Definição:** Serviço ou peça identificado após a aprovação do orçamento original, que exige nova aprovação do cliente antes de ser executado.
**Exemplo:** "Abriu o motor e a bomba d'água também tinha ido; mandei como reparo adicional."
**Fonte:** "autorizar reparos adicionais via aplicativo"

### Orçamento Complementar
**Estereótipo:** Entidade interna ao agregado
**Definição:** Orçamento emitido para cobrir reparos adicionais, apresentado ao cliente separadamente do orçamento original.
**Nota:** Existir como termo próprio responde à pergunta "o orçamento aprovado muda quando surge reparo adicional?" — não muda; nasce outro. Orçamento aprovado é acordo fechado.
**Fonte:** derivado

### Serviço Lançado
**Estereótipo:** Objeto de Valor interno ao agregado
**Definição:** Serviço do catálogo aplicado a uma ordem de serviço específica, com o preço vigente no instante do lançamento.
**Exemplo:** "Lancei o alinhamento na 1043."
**Nota:** Distingue-se de `Serviço` (catálogo) porque são conceitos diferentes que o enunciado nomeia igual. O preço é congelado para que reajuste futuro não altere acordo já firmado.
**Fonte:** "Inclusão dos serviços solicitados"

### Item Lançado
**Estereótipo:** Objeto de Valor interno ao agregado
**Definição:** Peça ou insumo aplicado a uma ordem de serviço específica, com quantidade e preço congelados no lançamento.
**Fonte:** "Possibilidade de incluir peças e insumos necessários"

---

## §5 Subdomínios de apoio e genéricos — verbetes

**Genéricos** — cadastro elementar e validações de formato:

### Cliente
**Estereótipo:** Entidade · raiz de agregado próprio
**Definição:** Pessoa física ou jurídica responsável por um veículo perante a oficina, que aprova orçamentos e recebe o veículo na entrega.
**Exemplo:** "O cliente ainda não aprovou o orçamento da 1043."
**Nota:** Aparece quatro vezes no enunciado com referências distintas — da oficina, da OS, do orçamento, do acompanhamento. É **um conceito exercendo papéis diferentes**. Tratá-lo como quatro conceitos descreveria a estrutura do texto, não o domínio.
**Fonte:** "Identificação do cliente por CPF/CNPJ"

### Veículo
**Estereótipo:** Entidade · raiz de agregado próprio
**Definição:** Automóvel apresentado à oficina para manutenção, descrito por marca, modelo e ano.
**Regra de reconhecimento:** identifica-se um veículo pela sua placa.
**Termo depreciado:** *cadastro de veículo* — cadastrar é ação, não conceito.
**Nota:** Como a identidade é a placa, um veículo já conhecido não é recriado a cada visita: vincula-se ao novo atendimento. É essa relação acumulada que constitui o histórico do veículo.
**Fonte:** "Cadastro de veículo (placa, marca, modelo, ano)"

### Placa
**Estereótipo:** Objeto de Valor
**Definição:** Código oficial de identificação de um veículo, imutável e sujeito a validação de formato.
**Fonte:** "Validação dos dados sensíveis (CPF/CNPJ, placa de veículo)"

### Identificação
**Estereótipo:** Objeto de Valor
**Definição:** Documento pelo qual um cliente é reconhecido — CPF para pessoa física, CNPJ para pessoa jurídica.
**Fonte:** "Identificação do cliente por CPF/CNPJ"

**Apoio** — construídos sob medida, sem diferencial competitivo:

### Serviço
**Estereótipo:** Entidade
**Definição:** Trabalho que a oficina oferece, com descrição e preço-base, independente de qualquer ordem de serviço.
**Extensão (exemplos do enunciado):** troca de óleo, alinhamento
**Não confundir com:** `Serviço Lançado`
**Exemplo:** "Cadastra balanceamento na tabela de serviços."
**Fonte:** "CRUD de serviços"

### Peça
**Estereótipo:** Entidade
**Definição:** Componente físico aplicado ao veículo, que nele permanece identificável.
**Exemplo:** filtro de óleo, pastilha de freio, correia

### Insumo
**Estereótipo:** Entidade
**Definição:** Material consumido durante a execução de um serviço, sem permanecer identificável no veículo.
**Exemplo:** óleo, estopa, graxa
**Nota:** O enunciado trata peças e insumos como um único fluxo. Ambas as palavras foram preservadas porque a oficina as distingue na fala e a distinção tem consequência (garantia atrelada ao veículo). Ver Q4.

### Item de Estoque
**Estereótipo:** conceito abrangente sobre `Peça` e `Insumo`
**Definição:** Peça ou insumo considerado sob a ótica da quantidade disponível.

### Estoque
**Estereótipo:** Entidade · raiz de agregado próprio
**Definição:** Quantidade disponível de cada peça e insumo na oficina.
**Nota:** Agregado separado da OS. A justificativa é a mesma regra de sempre: se a baixa ocorrer segundos após a execução, a oficina sobrevive — logo a consistência pode ser eventual, logo a fronteira é outra. Classificado como apoio, e não genérico, porque a baixa precisa referenciar a ordem que a autorizou.
**Fonte:** "CRUD de peças e insumos, com controle de estoque"

### Baixa
**Estereótipo:** Objeto de Valor · candidato a Evento de Domínio
**Definição:** Retirada de peça ou insumo do estoque em razão de aplicação em um veículo.
**Nota:** Ver Q3 — o instante da baixa ainda não está decidido.

---

## §6 Conceitos implícitos tornados explícitos

Evans, cap. 9: ouvir a linguagem, examinar o que soa desajeitado no discurso e promover a conceito explícito o que estava escondido. Os casos abaixo seguem esse procedimento, com a categoria do capítulo indicada.

### Transição e Regra de Transição — *Explicit Constraints*
A frase "alteração automática dos status conforme ações no sistema" é o desconforto que denuncia o conceito ausente. Sem nomear a transição, não existe lugar para a restrição morar: ela se dispersa em condicionais espalhadas. O cap. 9 trata restrições explícitas como conceitos de primeira classe, e o padrão *Specification* dá a forma canônica.

**O que a promoção rende:** a invariante "não se executa sem aprovação" passa a ser um objeto interrogável em vez de um `if`, e o conjunto das regras passa a **definir a fronteira do agregado** — o que é consistência imediata fica dentro, o que tolera atraso fica fora.

### Ciclo de vida da OS — *Processes as Domain Objects*
Os quatro processos da introdução e os seis status do requisito descrevem o mesmo ciclo. Reconhecê-lo como um processo único, com identidade, elimina a sinonímia e produz o `Histórico de Status`.

### Estoque — conceito ausente do levantamento inicial
Escondido no final de uma linha de requisito ("com controle de estoque"), não foi capturado pela extração de substantivos. É o conceito que liga catálogo a execução e responde diretamente à dor "falhas no controle de peças e insumos".

### Diagnóstico como artefato
Aparece no enunciado somente como etapa. Promovido a registro, torna-se o que justifica o orçamento perante o cliente — ou seja, o que dá defensabilidade à cobrança.

---

## §7 Linguagem descartada

Evans (cap. 2) trata a Ubiquitous Language como linguagem do time inteiro, negociada e sujeita a revisão. Registrar o que foi rejeitado impede que os termos retornem por reinvenção.

| Termo do enunciado | Motivo do descarte |
|---|---|
| CRUD | Vocabulário de implementação. Um gestor diz "cadastrar cliente" |
| Aplicativo, API, Swagger, JWT, Docker | Requisitos técnicos; nenhum sobrevive à remoção do software |
| Fluxos principais, Funcionalidades obrigatórias | Estrutura do documento de requisitos |
| Sistema, SINATES | Nome do produto; pertence ao README |
| Gestão interna, Gestão administrativa | Agrupamento de funcionalidades, não conceito |
| Processo, Fluxo | Sinonímia com `Status` |
| Autorização | Sinonímia com `Aprovação` |
| Cadastro | Ação, não conceito |
| Listagem, Detalhamento, Monitoramento | Operações de leitura sobre conceitos, não conceitos |
| Erros na priorização, falhas no controle, perda de histórico, ineficiência | Dores que motivam o projeto; pertencem à §1 |

**Critério aplicado:** a palavra sobrevive se o software desaparecer? Um mecânico em 1985, com papel carbono, dizia "ordem de serviço", "orçamento", "peça", "entrega". Nunca disse "CRUD".

As dores, contudo, foram produtivas como pista: "perda de histórico" levou aos históricos de cliente e veículo; "falhas no controle de peças" levou ao `Estoque`.

---

## §8 Questões em aberto

Cap. 1, *Crunching Knowledge*: o modelo é resultado de destilação contínua com o especialista, não de uma leitura de requisitos. Ambiguidade não resolvida permanece visível.

**Q1 — Qual o desfecho de uma ordem de serviço reprovada?**
Os seis status não preveem término negativo. Não há *Cancelada* nem *Recusada*. A oficina precisa devolver o veículo sem trabalho executado, e nenhum status descreve isso. Provavelmente falta um estado terminal no enunciado.

**Q2 — O tempo médio de execução mede o quê?**
*Em execução → Finalizada* mede produtividade da oficina. *Recebida → Entregue* mede experiência do cliente e inclui o tempo em que a OS esperou a resposta dele. A segunda leitura pune a oficina por demora do cliente. A escolha altera o significado político do indicador.

**Q3 — Em que instante a peça sai do estoque?**
Abertura, aprovação ou execução. Cada resposta produz comportamento diferente quando o cliente reprova ou demora. Se a baixa for na aprovação, surge a necessidade de um conceito de **reserva**.

**Q4 — Peça e insumo têm comportamento realmente distinto?**
Se a distinção nunca gera decisão diferente, os dois colapsam em um conceito com atributo de tipo.

**Q5 — O cliente é sempre o proprietário do veículo?**
Frotas, veículos de família e revendas quebram a suposição. Assumido que sim no MVP — decisão registrada, não fato observado.

**Q6 — Uma OS admite múltiplos orçamentos complementares?**
Três descobertas em momentos distintos geram três aprovações ou uma consolidada?

---

## §9 De/para português → inglês

O código-fonte do projeto é escrito em inglês. Esta seção fixa a tradução de cada termo para que a linguagem ubíqua sobreviva à mudança de idioma.

**Nota de fidelidade:** Evans (cap. 2) trata a Ubiquitous Language como linguagem única entre especialista e time. Traduzir o código para o inglês **quebra** essa unidade — o especialista do domínio fala português. A tradução é uma concessão a convenções de mercado, não uma decisão de modelagem. Esta tabela existe para limitar o dano: o mapeamento é fixo, um para um, e nenhum termo pode ser traduzido em código sem constar aqui.

### Conceitos do núcleo

| Português | Inglês | Nota |
|---|---|---|
| Ordem de Serviço | `ServiceOrder` | |
| Status | `ServiceOrderStatus` | enum |
| Mudança de Status | `StatusChange` | substantivo ainda não validado com o domínio |
| Histórico de Status | `StatusHistory` | |
| Diagnóstico | `Diagnosis` | |
| Orçamento | `Estimate` | termo padrão em oficina anglófona; `Quote` é alternativa |
| Aprovação | `Approval` | |
| Reprovação | `Rejection` | |
| Reparo Adicional | `AdditionalRepair` | |
| Orçamento Complementar | `SupplementaryEstimate` | |
| Serviço Lançado | `ServiceLineItem` | instância na OS, não o catálogo |
| Item Lançado | `MaterialLineItem` | |

### Status

| Português | Inglês |
|---|---|
| Recebida | `RECEIVED` |
| Em diagnóstico | `UNDER_DIAGNOSIS` |
| Aguardando aprovação | `AWAITING_APPROVAL` |
| Em execução | `IN_PROGRESS` |
| Finalizada | `COMPLETED` |
| Entregue | `DELIVERED` |
| Recusada | `REJECTED` |

### Apoio e genéricos

| Português | Inglês | Nota |
|---|---|---|
| Cliente | `Customer` | |
| Veículo | `Vehicle` | |
| Placa | `LicensePlate` | objeto de valor |
| Marca | `make` | termo automotivo padrão; não `brand` |
| Modelo | `model` | |
| Ano | `modelYear` | |
| Identificação | `TaxId` | objeto de valor que encapsula CPF/CNPJ |
| CPF | `Cpf` | **não traduzir** — termo próprio do ordenamento jurídico brasileiro |
| CNPJ | `Cnpj` | **não traduzir** — idem |
| Serviço (catálogo) | `Service` | |
| Peça | `Part` | |
| Insumo | `Supply` | |
| Material | `Material` | abrange peça e insumo, conforme quadro do Event Storming |
| Item de Estoque | `InventoryItem` | |
| Estoque | `Inventory` | agregado; `stock` para a quantidade |
| Baixa | `StockWithdrawal` | |
| Reserva | `Reservation` | |

### Atores

| Português | Inglês | Nota |
|---|---|---|
| Funcionário | `Employee` | termo genérico usado no quadro atual |
| Atendente | `ServiceAdvisor` | termo real do setor automotivo anglófono |
| Mecânico | `Technician` | uso profissional prevalece sobre `Mechanic` |
| Gestor | `Manager` | |

### Convenções derivadas

| Elemento | Padrão | Exemplo |
|---|---|---|
| Comando | verbo no imperativo | `RegisterVehicle` |
| Evento de domínio | particípio passado | `VehicleRegistered` |
| Política | `When<Evento>` | `WhenEstimateApproved` |
| Especificação | `<Regra>Specification` | `ApprovalRequiredSpecification` |
| Repositório | `<Agregado>Repository` | `VehicleRepository` |

### Termos transversais

Vocabulário técnico e jurídico, não linguagem de domínio. Fica em subseção própria porque não nasceu da fala do especialista — nenhum mecânico diz "trilha de auditoria". São termos que o sistema precisa nomear para cumprir obrigação legal e para se manter auditável, e misturá-los aos conceitos de negócio das tabelas anteriores daria a eles um estatuto que não têm.

| Português | Inglês | Nota |
|---|---|---|
| Trilha de auditoria | `AuditTrail` | termo técnico transversal |
| Dado pessoal | `PersonalData` | LGPD Art. 5º I — nunca traduzir como "sensitive data" |
| Anonimização | `Anonymization` | técnica de proteção, não evento de domínio |

### Comandos e eventos da fatia `Vehicle`

| Comando | Evento |
|---|---|
| `RegisterVehicle` | `VehicleRegistered` |
| `UpdateVehicle` | `VehicleUpdated` |
| `RemoveVehicle` | `VehicleRemoved` |

`UpdateVehicle` dispensa qualificador porque não existe outro tipo de atualização de veículo: a placa é imutável, e o que resta — marca, modelo e ano — muda em bloco. `Veículo` é subdomínio genérico (§3), então o vocabulário banal de cadastro é adequado aqui; a proibição de linguagem de implementação registrada na §7 vale para o núcleo, onde nomear mal esconde regra de negócio.

`RemoveVehicle` nomeia a intenção, não a técnica: o veículo sai do cadastro ativo. A anonimização é **como** essa saída é executada preservando o histórico exigido por obrigação legal e por garantia (Art. 16 I) — logo é termo transversal, e não evento de domínio. Ninguém na oficina aciona uma função chamada "anonimizar".

---

## §10 Índice alfabético

Aprovação §4 · Baixa §5 · Cliente §5 · Diagnóstico §4 · Estoque §5 · Histórico de Status §4 · Identificação §5 · Insumo §5 · Item de Estoque §5 · Item Lançado §4 · Ordem de Serviço §4 · Orçamento §4 · Orçamento Complementar §4 · Peça §5 · Placa §5 · Reparo Adicional §4 · Reprovação §4 · Regra de Transição §4 · Serviço §5 · Serviço Lançado §4 · Status §4 · Transição §4 · Veículo §5

---

## Referências

ABBOTT, R. J. Program Design by Informal English Descriptions. *Communications of the ACM*, v. 26, n. 11, p. 882–894, 1983.

EVANS, E. *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Boston: Addison-Wesley, 2003.

INTERNATIONAL ORGANIZATION FOR STANDARDIZATION. *ISO 10241-1:2011 — Terminological entries in standards — Part 1: General requirements and examples of presentation*. Genebra, 2011.

KHONONOV, V. *Learning Domain-Driven Design: Aligning Software Architecture and Business Strategy*. Sebastopol: O'Reilly, 2021.

VERNON, V. *Implementing Domain-Driven Design*. Boston: Addison-Wesley, 2013.
