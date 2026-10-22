# Cobertura de testes

Medida com JaCoCo 0.8.15, configurado em `server/pom.xml`.

## Como gerar o relatório

```bash
cd server
./mvnw verify
```

Relatório em `server/target/site/jacoco/index.html`. Use o wrapper: o build exige Maven 3.9+ e
Java 25, e o Docker precisa estar em execução — os testes de integração sobem um
`postgres:16-alpine` via Testcontainers.

`mvn test` não gera relatório: o `report` está ligado à fase `verify`, depois dos testes de
integração.

Surefire e Failsafe são instrumentados por agentes separados (`jacoco-unit.exec` e
`jacoco-integration.exec`), fundidos em `jacoco-merged.exec` no `verify`. Medir só uma das
execuções subestima a cobertura, porque os adaptadores de persistência só são exercidos pelos ITs.

Além do HTML, o `verify` produz `jacoco.xml` (para SonarQube ou CI) e `jacoco.csv` (uma linha por
pacote) no mesmo diretório. Tudo fica em `target/`, que não é versionado — um relatório presente
na árvore de trabalho pode ser de uma execução anterior.

## Convenção de nomes

| Sufixo | Plugin | Uso |
|---|---|---|
| `*Test` | Surefire | teste unitário, sem contexto Spring nem banco |
| `*IT` | Failsafe | teste de integração, com contexto Spring e PostgreSQL real |

## Limite mínimo

A execução `check-coverage` exige **80% de linhas por pacote** e falha o build quando violada,
com escopo declarado em regras de negócio e casos de uso:

```xml
<include>com.jacafi.tech.*.domain*</include>
<include>com.jacafi.tech.*.application*</include>
```

O curinga no fim inclui os subpacotes (`domain.entity`, `domain.exception`, `application.service`,
`application.port`), onde as classes de fato ficam. Sem ele, a regra alcançaria apenas os pacotes
terminados exatamente em `.domain` ou `.application`, o que deixava as seis fatias de fora.

Adaptadores, DTOs e configuração ficam fora do limite. Continuam medidos e aparecem no relatório,
mas não bloqueiam o build: são código de ligação, cuja garantia vem dos testes de integração.

## Situação

`./mvnw verify` com todos os testes passando (159 unitários e 21 de integração) e a regra de 80%
atendida nos 17 pacotes avaliados. Cobertura de linhas do projeto: **70,5%** (1862 de 2640). Por
fatia, considerando apenas `domain` e `application`:

| Fatia | Linhas cobertas |
|---|---|
| `laboroperation` | 100% |
| `shared` | 99% |
| `customer` | 98% |
| `inventory` | 93% |
| `vehicle` | 89% |
| `serviceorder` | 83% |

O número global fica abaixo do de cada fatia porque inclui DTOs, adaptadores e controllers, fora
do escopo do limite. O pacote mais próximo do limite é `serviceorder.application.service` (81,6%):
uma alteração nele sem testes correspondentes tende a derrubar o build.
