# ADR-003 — Backend Java com Spring Boot

## Status

Aceito.

## Contexto

O backend precisa expor APIs REST autenticadas, persistir dados no PostgreSQL, validar entradas, documentar contratos e executar testes automatizados. O time precisa de tecnologias maduras e adequadas a um monólito modular.

## Alternativas consideradas

- Node.js com Express: rápido para iniciar, mas não é a stack adotada pelo projeto nem oferece a mesma integração nativa com o ecossistema Java usado pela equipe.
- .NET: possui recursos equivalentes, porém exigiria outra stack e experiência operacional adicional.
- Quarkus: é uma opção Java válida, mas Spring Boot reduz risco por já prover as integrações usadas no projeto.

## Decisão

Serão utilizados Java 25 e Spring Boot 4. Spring é usado nos adaptadores e na composição da aplicação para web, segurança, persistência, validação, OpenAPI, observabilidade e testes. O domínio permanece independente do framework.

## Consequências

O projeto aproveita um ecossistema consolidado para as preocupações de infraestrutura sem acoplar as regras de negócio ao framework. A equipe deve manter dependências atualizadas, testar os contratos e evitar anotações de infraestrutura no domínio.
