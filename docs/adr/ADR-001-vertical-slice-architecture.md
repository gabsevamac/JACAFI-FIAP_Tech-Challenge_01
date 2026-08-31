# ADR-001 — Arquitetura Vertical Slice

## Status

Aceito.

## Contexto

O MVP reúne funcionalidades de clientes, veículos, catálogo, estoque, contas e ordens de serviço, desenvolvidas por mais de uma pessoa. A organização precisa reduzir acoplamento entre essas áreas e permitir evolução seletiva sem transformar o monólito inicial em microsserviços prematuramente.

## Alternativas consideradas

- Monólito em camadas técnicas globais: simples no início, mas concentra mudanças de domínios distintos nas mesmas pastas.
- Microsserviços desde o MVP: oferece isolamento de deploy, porém adiciona custo operacional e integrações distribuídas antes de haver demanda comprovada.
- Pacotes somente por entidade: reduz a coesão dos casos de uso que envolvem mais de uma entidade do mesmo contexto.

## Decisão

O sistema será um monólito modular em Vertical Slice Architecture. Cada fatia agrupa seu domínio, casos de uso e adaptadores. Dentro de cada fatia, Clean Architecture mantém as dependências direcionadas ao domínio: regras de negócio não dependem de Spring, JPA, HTTP ou PostgreSQL.

## Consequências

As mudanças ficam localizadas por capacidade de negócio e as fatias podem ser extraídas no futuro quando houver necessidade operacional real. A equipe deve preservar as fronteiras e integrar fatias por identificadores e portas explícitas, em vez de importar detalhes de persistência ou entidades entre elas.
