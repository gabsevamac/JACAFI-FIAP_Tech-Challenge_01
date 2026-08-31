# ADR-004 — Transactional Outbox para Auditoria e Notificações

## Status

Aceito.

## Contexto

Uma alteração de negócio deve manter o seu registro de auditoria e solicitar a notificação de status da OS sem depender da disponibilidade imediata de serviços externos. O envio de e-mail pode falhar temporariamente e não pode reverter nem duplicar a atualização da ordem de serviço.

## Alternativas consideradas

- Gravar auditoria e enviar e-mail diretamente no caso de uso: é menor, mas perde eventos quando a transação falha e aumenta a latência da API.
- Usar uma única tabela para auditoria e processamento: mistura um histórico imutável com estado operacional mutável, dificultando retenção e rastreabilidade.
- Introduzir um broker externo no MVP: aumenta a resiliência, mas adiciona infraestrutura não exigida para a fase atual.

## Decisão

As transações de negócio gravam eventos em uma tabela `event_outbox`. Um processamento assíncrono consome esses eventos, registra a `audit_trail` imutável e solicita e-mails de status pelo Resend. O processamento é idempotente, possui tentativas limitadas e mantém dados pessoais fora do payload de auditoria.

## Consequências

As APIs permanecem responsivas e os efeitos secundários são recuperáveis após falhas temporárias. A aplicação passa a ter uma rotina de processamento e estados de entrega a monitorar. Um broker poderá substituir o consumidor local futuramente sem alterar os casos de uso.
