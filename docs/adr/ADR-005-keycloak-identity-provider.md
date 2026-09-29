# ADR-005 — Keycloak como Provedor de Identidade e Duas Roles de Negócio

## Status

Aceito.

## Contexto

A fatia `auth` concentrava 38 arquivos e 1.082 linhas para resolver um problema genérico: cadastrar contas, guardar hash de senha, emitir e validar JWT e expor um CRUD administrativo de usuários. Nada disso é regra de oficina, e a implementação própria não oferecia refresh token, revogação, logout, política de senha, troca ou recuperação de senha, bloqueio por tentativas nem segundo fator. Um token emitido permanecia válido até expirar, sem forma de invalidação.

O modelo de papéis também estava mais granular do que o negócio exige hoje. Havia cinco roles — `ADMIN`, `MANAGER`, `SERVICE_ADVISOR`, `TECHNICIAN` e `CUSTOMER` —, mas as políticas de acesso tratavam as quatro primeiras como um único conjunto operacional na maior parte dos casos, e as distinções remanescentes não correspondiam a nenhuma decisão de negócio real.

Ao mesmo tempo, uma parte da fatia `auth` não era genérica: o vínculo entre uma conta e o cliente que ela representa (`user_accounts.customer_id`), com a invariante de que a role de cliente existe exatamente quando esse vínculo existe. Esse é um fato da oficina, não do provedor de identidade.

## Alternativas consideradas

- **Manter a autenticação própria e apenas reduzir as roles.** Resolve o excesso de papéis, mas mantém no repositório o hash de senha, a emissão de token e o CRUD de contas, além de continuar sem refresh, revogação e política de senha.
- **Mover também as políticas de acesso para o Keycloak, via Authorization Services.** Levaria as decisões de autorização para fora do código, sem teste unitário e sem apoio do compilador. A verificação "o cliente só lê a própria OS" é uma checagem de propriedade sobre dado de negócio; expressá-la como política declarativa em serviço externo aumenta a complexidade em vez de reduzi-la.
- **Guardar o `customerId` como atributo do usuário no Keycloak, entregue em claim.** É a opção com menos código: uma coluna vira um claim, sem tabela nem consulta. Foi recusada porque torna o provedor de identidade o dono de um dado da oficina: excluir e recriar o usuário no Keycloak perde o vínculo em silêncio, nenhuma restrição de banco o protege, e o cadastro de cliente passaria a exigir edição de atributo no console do IdP.
- **Persistir o Keycloak local em Postgres.** Deixaria o ambiente de desenvolvimento mais próximo de produção, mas exige um segundo banco e script de inicialização. Para um ambiente local cujo realm é versionado, o custo não se paga.

## Decisão

### Duas roles

O domínio passa a ter duas roles: `EMPLOYEE` e `CUSTOMER`. `EMPLOYEE` alcança todos os recursos da aplicação — o antigo `ADMIN` está contido nela. `CUSTOMER` permanece restrito aos próprios dados, com as políticas de acesso do cliente inalteradas. A migration `V10` consolida as quatro roles operacionais em `EMPLOYEE` e reduz a restrição de domínio no banco.

### Keycloak como provedor de identidade

Passa para o Keycloak tudo que é IAM: usuários, credenciais, roles, emissão e validação de token, e a administração de contas. A aplicação torna-se um *resource server* OAuth2: valida o token pelo JWKS do realm, sem segredo compartilhado. Saem do repositório a fatia `auth` inteira, as três dependências `jjwt`, o `JWT_SECRET`, as tabelas `user_accounts` e `user_account_roles` (migration `V12`) e o endpoint `POST /api/v1/auth/login`.

As roles chegam no claim `realm_access.roles` do Keycloak (`employee`, `customer`) e são convertidas para o enum de domínio por `KeycloakJwtAuthenticationConverter`, uma vez por requisição. O realm fica versionado em [keycloak/jacafi-realm.json](../../keycloak/jacafi-realm.json) e é importado na subida do container, o que também tira o hash da senha de desenvolvimento das migrations.

### O que permanece na aplicação

- **As cinco políticas de acesso por fatia.** São autorização de domínio, não autenticação. Continuam em Java, testáveis por teste unitário.
- **A abstração `CurrentAuthenticatedUserPort` / `AuthenticatedUser`**, movida para `shared/security` junto de `Role` e das exceções de acesso. É o que permitiu trocar o provedor sem tocar em nenhuma fatia de negócio: apenas o adaptador mudou.
- **O vínculo identidade ↔ cliente**, agora em `customer_identities` (migration `V11`), uma tabela do slice `customer` com chave primária no `sub` do Keycloak e índice único no cliente. O `PUT /api/v1/customers/{customerId}/identity`, restrito a funcionário, estabelece o vínculo. O conversor de token resolve `sub` → `customerId` uma vez por requisição, e um cliente sem vínculo recebe `403`, não `500`.
- **A auditoria.** `JwtAuditorAware` continua registrando o ator; passa a usar o `preferred_username` do token.

## Consequências

O projeto perde 1.082 linhas de código de produção, cerca de 370 de teste, 3 dependências, 2 tabelas e 1 segredo gerenciado, e ganha refresh token, revogação, logout, política de senha, recuperação de senha, bloqueio por tentativas e a possibilidade de segundo fator sem escrever código.

Em contrapartida, o ambiente local passa a ter um container a mais, e subir a aplicação depende da saúde do Keycloak. O realm local é efêmero: o H2 do container não é persistido e o realm é reimportado a cada subida, de modo que o arquivo versionado é a única fonte da verdade. Os `id` dos usuários de desenvolvimento estão fixados no export para que o `sub` — e portanto os vínculos em `customer_identities` — permaneçam estáveis entre reinícios. Usuários criados manualmente pelo console não sobrevivem a um `docker compose down`.

O `issuer` que o Keycloak emite é o endereço público (`http://localhost:8081/realms/jacafi`), enquanto a aplicação busca o JWKS pelo endereço interno da rede do Compose. Por isso `issuer-uri` e `jwk-set-uri` são configurados separadamente: o primeiro valida o claim `iss`, o segundo obtém as chaves.

Nos testes, `TestSecurityConfiguration` substitui o decodificador do JWKS por um de chave simétrica e `TestTokens` emite tokens com o mesmo formato do realm. O conversor de roles, a cadeia de filtros e as políticas de acesso são exercitados sem provedor de identidade em execução.
