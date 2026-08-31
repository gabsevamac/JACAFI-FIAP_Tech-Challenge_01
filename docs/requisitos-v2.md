# Requisitos Funcionais

| Requisito | Descrição |
|---|---|
| Sistema de autenticação | Necessário que usuário possa realizar o registro e o login em nosso sistema. |
| Gerenciamento de clientes | O sistema deve permitir que os funcionários busquem, cadastrem e atualizem os dados dos clientes. |
| Gerenciamento de veículos | Necessário que funcionários possam cadastrar e modificar veículos no sistema. |
| Gerenciamento de Ordem de Serviço | O sistema deve permitir criar, diagnosticar, alterar os status da manutenção (Recebida, Em Diagnóstico, Em Execução, Finalizada, Entregue) e fechar as Ordens de Serviço. |
| Gerenciamento de serviços | Necessário que os atores (cliente ou funcionário) possam registrar e solicitar a inclusão de novos serviços (exemplo: troca de óleo, alinhamento, etc.). |
| Acompanhamento de manutenção | Necessário permitir que usuário acompanhe o andamento da manutenção do seu veículo. |
| Controle de Estoque | O CRUD de peças e insumos deve contemplar o controle de estoque. |
| Reserva de Peças | Funcionalidades de reserva temporária atreladas a orçamentos e cancelamento automático de reservas. |
| Inclusão de insumos | Necessário que mecânicos possam adicionar à ordem de serviço todos os materiais necessários para realizar a manutenção. |
| Orçamento Automático e Decisão | O sistema deve gerar um orçamento automático, com base no serviço da Ordem de Serviço e insumos. |
| Aceite/Recusa de Orçamento | O sistema deve permitir o usuário realizar o aceite ou recusa do orçamento gerado para o serviço. |
| Notificação de Clientes | O sistema deve disparar notificações ao cliente sobre a geração de orçamentos, alterações críticas de status e momento de entrega do veículo. |
| Monitoramento de Tempo de Execução | O sistema deve calcular e monitorar o tempo médio de execução dos serviços prestados na oficina. |

# Requisitos Não Funcionais

| Requisito | Descrição |
|---|---|
| Autenticação JWT | Sistema deve realizar autenticação do usuário utilizando tokens JWT. |
| Validação de dados sensíveis | Sistema deve realizar a validação de dados sensíveis (CPF/CNPJ, placa do veículo). |
| Versionamento com Docker | Sistema deve apresentar versionamento utilizando Docker Compose. |
| Arquitetura em Camadas (DDD) | O back-end deve ser desenvolvido em estrutura monolítica aplicando a arquitetura em camadas e princípios de Domain-Driven Design (DDD). |
| Testes Automatizados | Implementação de testes unitários e de integração com cobertura mínima de 80% nos domínios críticos do sistema. |
| Concorrência de Banco de Dados | As operações de baixa, reserva e estorno de insumos no banco de dados devem utilizar controle de concorrência transacional seguro. |
| Logs e Rastreabilidade | O sistema deve registrar uma trilha de auditoria para cada mudança de status na Ordem de Serviço, salvando data/hora (timestamp) e o responsável pela ação. |
| Design simples | O sistema deve ter um design simples e intuitivo para uso facilitado para qualquer usuário, com uma curva de aprendizado baixa. |
| Baixa latência | O sistema deve funcionar de forma rápida e otimizada, para encaixar no fluxo já existente sem que o usuário fique com a sensação que está perdendo tempo. |
