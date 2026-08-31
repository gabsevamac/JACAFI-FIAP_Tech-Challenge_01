# Histórias de Usuário — Sistema de Gestão da Oficina
### Tech Challenge — Fase 1

---

## Épico 1 — Gestão de Clientes

### US01 — Cadastrar cliente
**Como** funcionário da oficina,
**quero** cadastrar um cliente informando seus dados,
**para que** seja possível identificá-lo e associá-lo às ordens de serviço.

**Critérios de aceite:**
- Deve ser possível cadastrar um cliente utilizando CPF ou CNPJ.
- O CPF/CNPJ informado deve ser validado.
- Não deve ser permitido cadastrar um cliente com CPF/CNPJ já existente.
- Após o cadastro, o cliente deve estar disponível para consulta.

### US02 — Identificar cliente
**Como** funcionário da oficina,
**quero** buscar um cliente por CPF/CNPJ,
**para** identificá-lo durante o atendimento e a criação da ordem de serviço.

**Critérios de aceite:**
- Deve ser possível buscar um cliente pelo CPF/CNPJ.
- Quando o cliente for encontrado, seus dados devem ser apresentados.
- Quando o cliente não for encontrado, o sistema deve informar que não existe cadastro para os dados informados.
- O funcionário deve poder realizar o cadastro de um novo cliente quando necessário.

---

## Épico 2 — Gestão de Veículos

### US03 — Cadastrar veículo
**Como** funcionário da oficina,
**quero** cadastrar um veículo associado a um cliente,
**para** manter o histórico dos veículos atendidos pela oficina.

**Critérios de aceite:**
- Deve ser possível informar placa, marca, modelo e ano.
- A placa deve ser validada.
- O veículo deve estar associado a um cliente cadastrado.
- Não deve ser permitido cadastrar um veículo com uma placa já cadastrada.
- Após o cadastro, o veículo deve estar disponível para consulta.

### US04 — Identificar veículo
**Como** funcionário da oficina,
**quero** buscar um veículo pela sua placa,
**para** identificá-lo durante o atendimento.

**Critérios de aceite:**
- Deve ser possível buscar um veículo pela placa.
- Quando o veículo for encontrado, seus dados devem ser apresentados.
- O cliente associado ao veículo deve ser identificado.
- Quando o veículo não for encontrado, o sistema deve informar que não existe cadastro para a placa informada.

---

## Épico 3 — Criação da Ordem de Serviço

### US05 — Abrir ordem de serviço
**Como** funcionário da oficina,
**quero** abrir uma ordem de serviço para um cliente e seu veículo,
**para** registrar o atendimento e iniciar o processo de manutenção.

**Critérios de aceite:**
- Deve ser possível criar uma OS associada a um cliente e a um veículo.
- O cliente deve estar previamente identificado.
- O veículo deve estar previamente identificado.
- A OS deve possuir um identificador único.
- Uma nova OS deve iniciar com o status Recebida.
- A OS criada deve estar disponível para consulta.

### US06 — Incluir serviço na ordem de serviço
**Como** funcionário da oficina,
**quero** adicionar serviços à ordem de serviço,
**para** registrar os serviços que deverão ser realizados no veículo.

**Critérios de aceite:**
- Deve ser possível selecionar um serviço cadastrado.
- Deve ser possível adicionar um ou mais serviços à OS.
- Os serviços adicionados devem ficar associados à OS.
- O valor do orçamento deve ser recalculado quando um serviço for adicionado.

### US07 — Iniciar diagnóstico do veículo
**Como** funcionário da oficina,
**quero** iniciar o diagnóstico do veículo,
**para** identificar os problemas e necessidades de manutenção.

**Critérios de aceite:**
- A OS deve estar disponível para início do diagnóstico.
- O funcionário deve conseguir iniciar o diagnóstico.
- Ao iniciar o diagnóstico, a OS deve assumir o status Em diagnóstico.
- O sistema deve permitir registrar as informações obtidas durante o diagnóstico.

### US08 — Finalizar diagnóstico
**Como** funcionário da oficina,
**quero** finalizar o diagnóstico do veículo,
**para** registrar os problemas identificados e definir os serviços e materiais necessários.

**Critérios de aceite:**
- O diagnóstico deve ter sido iniciado anteriormente.
- O funcionário deve conseguir registrar o resultado do diagnóstico.
- O diagnóstico deve poder indicar serviços, peças e insumos necessários.
- Após a conclusão do diagnóstico, a OS deve avançar para a etapa de orçamento/aprovação.

---

## Épico 4 — Gestão de Peças, Insumos e Estoque

### US09 — Cadastrar peça ou insumo
**Como** funcionário da oficina,
**quero** cadastrar peças e insumos,
**para** manter um catálogo dos materiais utilizados pela oficina.

**Critérios de aceite:**
- Deve ser possível cadastrar peças.
- Deve ser possível cadastrar insumos.
- O material deve possuir as informações necessárias para sua identificação.
- Não deve ser permitido cadastrar materiais com identificadores inválidos ou duplicados.
- Materiais cadastrados devem ficar disponíveis para utilização nas ordens de serviço.

### US10 — Adicionar material ao estoque
**Como** funcionário da oficina,
**quero** adicionar peças e insumos ao estoque,
**para** manter atualizada a quantidade disponível de materiais.

**Critérios de aceite:**
- Deve ser possível adicionar peças ao estoque.
- Deve ser possível adicionar insumos ao estoque.
- A quantidade disponível deve ser atualizada após a entrada do material.
- Não deve ser possível adicionar uma quantidade inválida ao estoque.

### US11 — Consultar estoque
**Como** funcionário da oficina,
**quero** consultar o estoque de peças e insumos,
**para** verificar a disponibilidade dos materiais.

**Critérios de aceite:**
- Deve ser possível consultar os materiais cadastrados.
- Deve ser possível visualizar a quantidade disponível de cada material.
- Materiais sem estoque devem ser identificados.
- Deve ser possível consultar o catálogo de materiais.

### US12 — Reservar material para uma OS
**Como** funcionário da oficina,
**quero** reservar peças e insumos necessários para uma OS,
**para** garantir a disponibilidade dos materiais durante a execução dos serviços.

**Critérios de aceite:**
- O material deve estar cadastrado no estoque.
- Deve existir quantidade suficiente disponível para realizar a reserva.
- A reserva deve estar associada à OS.
- A quantidade disponível deve ser atualizada após a reserva.
- Não deve ser possível reservar uma quantidade superior à disponível.

### US13 — Desfazer reserva de material
**Como** funcionário da oficina,
**quero** desfazer a reserva de peças e insumos,
**para** devolver ao estoque materiais que não serão utilizados.

**Critérios de aceite:**
- Deve existir uma reserva associada à OS.
- Deve ser possível desfazer uma reserva.
- A quantidade reservada deve voltar a ficar disponível no estoque.
- O material não deve continuar vinculado à reserva após o cancelamento.

### US14 — Consumir material durante a execução
**Como** funcionário da oficina,
**quero** registrar o consumo das peças e insumos utilizados,
**para** manter o estoque atualizado durante a execução dos serviços.

**Critérios de aceite:**
- O material deve estar reservado para a OS.
- Deve ser possível registrar a quantidade efetivamente utilizada.
- A quantidade consumida deve ser retirada da disponibilidade do estoque.
- Não deve ser possível consumir uma quantidade superior à quantidade reservada.

### US15 — Notificar estoque zerado
**Como** administrador da oficina,
**quero** ser notificado quando um material atingir estoque zero,
**para** tomar providências para sua reposição.

**Critérios de aceite:**
- O sistema deve identificar quando a quantidade disponível de um material chegar a zero.
- O administrador deve receber uma notificação.
- A notificação deve identificar qual material atingiu estoque zero.

---

## Épico 5 — Orçamento

### US16 — Gerar orçamento da OS
**Como** funcionário da oficina,
**quero** gerar automaticamente o orçamento da OS,
**para** apresentar ao cliente o valor dos serviços, peças e insumos necessários.

**Critérios de aceite:**
- O orçamento deve considerar os serviços associados à OS.
- O orçamento deve considerar as peças e insumos necessários.
- O valor total deve ser calculado automaticamente.
- O orçamento deve estar associado à respectiva OS.
- O orçamento deve poder ser recalculado quando seus itens forem alterados.

### US17 — Enviar orçamento ao cliente
**Como** funcionário da oficina,
**quero** enviar o orçamento ao cliente,
**para** que ele possa analisar e autorizar os serviços.

**Critérios de aceite:**
- Deve existir um orçamento para a OS.
- O orçamento deve ser disponibilizado ao cliente.
- O cliente deve conseguir consultar as informações do orçamento.
- Após o envio do orçamento, a OS deve assumir o status Aguardando aprovação.

### US18 — Aprovar orçamento
**Como** cliente,
**quero** aprovar o orçamento da minha OS,
**para** autorizar a execução dos serviços.

**Critérios de aceite:**
- O cliente deve conseguir visualizar o orçamento.
- O cliente deve conseguir aprovar o orçamento.
- A aprovação deve ficar registrada.
- Após a aprovação, a OS deve avançar para o status Em execução, conforme o fluxo definido.

### US19 — Reprovar orçamento
**Como** cliente,
**quero** reprovar o orçamento da minha OS,
**para** informar que não autorizo a execução dos serviços propostos.

**Critérios de aceite:**
- O cliente deve conseguir visualizar o orçamento.
- O cliente deve conseguir reprovar o orçamento.
- A reprovação deve ficar registrada.
- Após a reprovação, a OS deve assumir o estado definido para o orçamento recusado.

---

## Épico 6 — Execução da Ordem de Serviço

### US20 — Iniciar execução dos serviços
**Como** funcionário da oficina,
**quero** iniciar a execução dos serviços autorizados,
**para** registrar o início da manutenção do veículo.

**Critérios de aceite:**
- O orçamento deve ter sido aprovado.
- Os materiais necessários devem estar disponíveis ou reservados.
- O funcionário deve conseguir iniciar a execução.
- A OS deve assumir o status Em execução.

### US21 — Registrar execução dos serviços
**Como** funcionário da oficina,
**quero** registrar os serviços executados,
**para** manter o histórico do trabalho realizado no veículo.

**Critérios de aceite:**
- O serviço deve estar associado à OS.
- O funcionário deve conseguir registrar a execução do serviço.
- Deve ser possível registrar a conclusão de cada serviço.
- Os serviços executados devem ficar disponíveis no histórico da OS.

### US22 — Concluir ordem de serviço
**Como** funcionário da oficina,
**quero** concluir a ordem de serviço,
**para** indicar que todos os serviços foram finalizados.

**Critérios de aceite:**
- Todos os serviços previstos devem estar concluídos.
- Os materiais utilizados devem estar devidamente registrados.
- A OS deve assumir o status Finalizada.
- O tempo de execução deve ser calculado.
- O cliente deve ser notificado para realizar a retirada do veículo.

---

## Épico 7 — Entrega e Acompanhamento

### US23 — Entregar veículo ao cliente
**Como** funcionário da oficina,
**quero** registrar a entrega do veículo,
**para** finalizar o atendimento da ordem de serviço.

**Critérios de aceite:**
- A OS deve estar com status Finalizada.
- O funcionário deve conseguir registrar a entrega do veículo.
- A entrega deve ficar registrada na OS.
- Após a entrega, a OS deve assumir o status Entregue.

### US24 — Acompanhar ordem de serviço
**Como** cliente,
**quero** consultar o status da minha ordem de serviço,
**para** acompanhar o andamento da manutenção do meu veículo.

**Critérios de aceite:**
- O cliente deve conseguir consultar sua OS por meio da API.
- Deve ser apresentado o status atual da OS.
- Deve ser possível acompanhar a evolução do atendimento.
- O cliente não deve conseguir consultar ordens de serviço de outros clientes.
- O status apresentado deve corresponder ao estado atual da OS.

---

## Épico 8 — Gestão Administrativa

### US25 — Gerenciar serviços
**Como** funcionário da oficina,
**quero** cadastrar, consultar, alterar e excluir serviços,
**para** manter atualizado o catálogo de serviços oferecidos pela oficina.

**Critérios de aceite:**
- Deve ser possível cadastrar um serviço.
- Deve ser possível consultar os serviços cadastrados.
- Deve ser possível alterar os dados de um serviço.
- Deve ser possível excluir um serviço quando não houver impedimentos.
- Os serviços cadastrados devem poder ser utilizados nas ordens de serviço.

### US26 — Consultar ordens de serviço
**Como** funcionário da oficina,
**quero** listar e consultar o detalhamento das ordens de serviço,
**para** acompanhar os atendimentos realizados pela oficina.

**Critérios de aceite:**
- Deve ser possível listar as ordens de serviço.
- Deve ser possível consultar o detalhamento de uma OS.
- O detalhamento deve apresentar cliente e veículo.
- O detalhamento deve apresentar os serviços associados.
- O detalhamento deve apresentar peças e insumos associados.
- O detalhamento deve apresentar o orçamento.
- O detalhamento deve apresentar o status atual da OS.

### US27 — Monitorar tempo médio dos serviços
**Como** gestor da oficina,
**quero** consultar o tempo médio de execução dos serviços,
**para** acompanhar a eficiência operacional da oficina.

**Critérios de aceite:**
- O sistema deve registrar o tempo de execução dos serviços.
- Deve ser possível consultar o tempo de execução de serviços concluídos.
- Deve ser possível calcular o tempo médio de execução.
- O cálculo deve considerar os serviços finalizados.

---

## Épico 9 — Segurança

### US28 — Autenticar funcionário
**Como** funcionário da oficina,
**quero** me autenticar no sistema,
**para** acessar as funcionalidades administrativas de forma segura.

**Critérios de aceite:**
- As APIs administrativas devem exigir autenticação.
- O sistema deve utilizar JWT para autenticação.
- Usuários não autenticados não devem conseguir acessar funcionalidades administrativas.
- O acesso deve ser permitido somente quando as credenciais forem válidas.
