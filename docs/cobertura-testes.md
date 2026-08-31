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
<include>com.jacafi.tech.*.domain</include>
<include>com.jacafi.tech.*.application</include>
```

Adaptadores, DTOs e configuração ficam fora do limite. Continuam medidos e aparecem no relatório,
mas não bloqueiam o build: são código de ligação, cuja garantia vem dos testes de integração.

**Limitação conhecida.** Os padrões casam com o nome completo do pacote, e nenhuma fatia tem
pacote terminando exatamente em `.domain` ou `.application` — as classes ficam em `domain.entity`,
`domain.exception`, `application.service` e `application.port`. A regra hoje alcança apenas
`com.jacafi.tech.shared.domain` e `com.jacafi.tech.shared.application`; o limite de 80% não está
sendo aplicado às seis fatias. Estender exige o sufixo curinga (`com.jacafi.tech.*.domain*`), o
que derruba o build no estado atual, então a correção deve vir junto com os testes que fecham a
lacuna. O `<exclude>com.jacafi.tech.features.*</exclude>` da mesma regra é resíduo de uma
estrutura de pacotes anterior.

## Situação

`./mvnw verify` com todos os testes passando. Cobertura de linhas do projeto: **68,6%**
(1907 de 2779). Por fatia, considerando apenas `domain` e `application`:

| Fatia | Linhas cobertas |
|---|---|
| `shared` | 99% |
| `customer` | 96% |
| `auth` | 95% |
| `vehicle` | 89% |
| `serviceorder` | 80% |
| `inventory` | 77% |
| `servicecatalog` | 71% |

O número global fica abaixo do de cada fatia porque inclui DTOs, adaptadores e controllers, fora
do escopo do limite. As lacunas de `servicecatalog` e `inventory` são o alvo do próximo
incremento de testes.
