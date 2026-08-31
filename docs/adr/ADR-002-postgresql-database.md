# ADR-002 — Banco de Dados PostgreSQL

## Status

Aceito.

## Contexto

Clientes, veículos, estoque e ordens de serviço possuem relacionamentos e regras de consistência. A aprovação do orçamento, a reserva de itens e a auditoria exigem persistência transacional, restrições e consultas previsíveis.

## Alternativas consideradas

- MySQL: atende ao modelo relacional, mas PostgreSQL oferece recursos de JSON, índices e restrições mais adequados aos dados de auditoria do projeto.
- Banco NoSQL: flexibiliza o schema, mas enfraquece as relações e transações necessárias entre OS, estoque e clientes.
- H2 apenas: é útil para testes locais, porém não é uma base adequada ao ambiente executado pelo Docker Compose.

## Decisão

Será utilizado PostgreSQL 16. O modelo relacional usa chaves, restrições e índices; Flyway versiona o schema com migrações no formato `VNN_YYYYMMDD__short_description.sql`.

## Consequências

Operações críticas podem preservar consistência transacional e a base suporta consultas por documentos, placas e estado da OS. Alterações de modelo passam obrigatoriamente por uma nova migração, que deve ser compatível com uma base já existente.
