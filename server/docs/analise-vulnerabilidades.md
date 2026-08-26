# Análise de Vulnerabilidades — SINATES

**Data da análise:** 2026-08-26
**Escopo:** código da aplicação, dependências e imagem de container.

## Nota de método

Um relatório de scan não é a saída da ferramenta. A ferramenta produz candidatos; o
relatório é a **triagem** deles — cada achado classificado como procedente, mitigado ou
falso positivo, **com o motivo**. Uma lista sem triagem transfere o trabalho para quem lê e
não afirma nada sobre a segurança do sistema.

## Frentes e cobertura

| Frente | Ferramenta | Estado | Roda no build padrão? |
|---|---|---|---|
| Código (SAST) | SpotBugs 4.10.4 + FindSecBugs 1.14.0 | executado | sim, `mvn verify` |
| Dependências (SCA) | OWASP Dependency-Check 13.0.0 | **configurado, não executado** | não — perfil `security-scan` |
| Imagem de container | Trivy | **não executado** | não — fora do Maven |

As três se complementam e nenhuma vê o campo da outra: SAST encontra o que escrevemos, SCA
o que importamos, e o scan de imagem o que a imagem base traz.

### O que ainda não foi executado, e por quê

**Dependency-Check** exige chave de API do NVD (gratuita) e a primeira execução baixa a base
completa de CVEs, levando dezenas de minutos. Está configurado e pronto:

```bash
mvn -Psecurity-scan verify -Dnvd.api.key=SUA_CHAVE
```

**Trivy** não é plugin Maven e não estava instalado no ambiente. Comando previsto:

```bash
docker build -t sinates:scan ./server
trivy image --severity HIGH,CRITICAL sinates:scan
```

Ambos são pendências conhecidas, não omissões.

---

## SAST — SpotBugs + FindSecBugs

**46 achados**, com `effort=Max` e `threshold=Low` — a configuração mais ruidosa de
propósito, para que a triagem seja nossa e não da ferramenta.

| Categoria | Qtd. |
|---|---|
| SECURITY | 29 |
| BAD_PRACTICE | 8 |
| MALICIOUS_CODE | 7 |
| STYLE | 2 |

### 1. `SPRING_CSRF_PROTECTION_DISABLED` — rank 10, prioridade 1

**Local:** `SecurityConfig`
**Veredito: mitigado por arquitetura, aceito.**

É o achado de maior severidade do relatório, e é esperado. A API é **stateless**, autentica
por *bearer token* no header `Authorization` e não usa cookie de sessão. CSRF depende de o
navegador anexar credencial automaticamente a uma requisição forjada — o que ocorre com
cookie, não com header, porque o header precisa ser escrito por JavaScript e isso está sob
a política de mesma origem.

**Condição que invalidaria esta análise:** passar a aceitar o JWT por cookie. Nesse caso o
CSRF volta a ser explorável e a proteção precisa ser reativada.

### 2. `CRLF_INJECTION_LOGS` — 9 ocorrências, rank 15

**Veredito: 6 procedentes (corrigidas), 3 falsos positivos.**

*Log forging* (CWE-117): valor com quebra de linha permite a quem o enviou anexar linhas
próprias ao log — forjando uma entrada plausível, com o traceId de outra pessoa, no meio de
um incidente. As linhas forjadas são indistinguíveis das reais porque **são** reais.

**Procedentes** — `GlobalExceptionHandler`: registra `e.getMessage()`, e várias exceções do
framework citam o valor submetido na mensagem, então entrada do cliente chega ali sem
ninguém ter logado de propósito. **Corrigido** com `LogSafe.value()`, que substitui quebras
de linha e trunca em 500 caracteres.

**Falsos positivos** — os três casos de uso de veículo registram apenas `UUID` e a placa
**mascarada**, derivada de valor que passou pelo regex `^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$`.
Não há como conter quebra de linha.

### 3. `XSS_SERVLET` — rank 12

**Local:** `SecurityProblemDetailHandler`
**Veredito: falso positivo.**

A ferramenta sinaliza escrita direta no `HttpServletResponse`. O conteúdo é servido como
`application/problem+json` — não interpretado como HTML — e o único valor de origem externa,
o URI da requisição, passa por escape de string JSON antes de ser escrito.

O código escreve JSON à mão de propósito: Jackson 2 e Jackson 3 estão ambos no classpath, e
um filtro de segurança é o lugar errado para depender de qual vence.

### 4. `SPRING_ENDPOINT` — 12 ocorrências, `SERVLET_HEADER` — 1

**Veredito: informativos, sem ação.**

Não são defeitos. O FindSecBugs os emite para marcar superfície de ataque — "aqui entra
dado externo" — e servem de índice para revisão manual, não de achado.

### 5. `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` — 7 ocorrências, rank 18

**Veredito: procedente de baixo impacto, aceito.**

Objeto mutável armazenado ou devolvido sem cópia. O caso real é `PageParameters`, que é um
`record` recebendo `List<String> sort` — records não copiam defensivamente. Os demais são
beans injetados pelo Spring, de instância única e efetivamente imutáveis.

Não há caminho de exploração: `PageParameters` é construído pelo binding a cada requisição e
descartado ao fim dela. `PageQuery` e `PageResult`, que atravessam camadas, **fazem**
`List.copyOf`.

### 6. `CT_CONSTRUCTOR_THROW` — 6 ocorrências, rank 16

**Veredito: procedente, mitigado pela linguagem.**

Construtor que lança permite ataque por finalizador. `Object.finalize()` está *deprecated for
removal* desde o Java 18 e a finalização foi desabilitada por padrão no Java 18+, então o
vetor não existe nesta JVM. Manter validação no construtor é o que impede objeto em estado
inválido, que é o risco maior.

### 7. `IMPROPER_UNICODE` — rank 15

**Local:** `SortableFields`
**Veredito: falso positivo.**

`toUpperCase` com `Locale.ROOT` — a forma que evita a armadilha do *turkish I* — e o
resultado alimenta um `switch` de lista branca com dois valores. Qualquer outra coisa é
rejeitada.

### 8. `SE_NO_SERIALVERSIONID`, `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR`, `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION`

**Veredito: sem impacto de segurança.** Estilo e boa prática. Ficam registrados para
higiene, sem prioridade.

---

## Controles verificados por teste, não por ferramenta

Os controles abaixo têm teste automatizado que reprova o build se forem removidos — o que é
uma garantia mais forte do que um scan, porque não depende de a ferramenta ter um detector
para eles.

| Controle | Onde é provado |
|---|---|
| Erro não vaza stack trace, SQL, nome de índice ou de classe | `ErrorLeakageIT`, `UnhandledExceptionTest` |
| Erro não devolve o dado pessoal submetido (CPF, placa) | `ErrorLeakageIT` |
| 401 não distingue usuário inexistente de senha errada | `ErrorLeakageIT` |
| Log nunca contém placa íntegra | `LogMaskingTest` |
| Ordenação não aceita campo fora da lista branca | `PageParametersTest`, `PagingContractIT` |
| Page size tem teto, recusado e não truncado | `PageParametersTest`, `PagingContractIT` |
| Registro removido não aparece em consulta | `VehicleRepositoryAdapterIT`, `AuditingIT` |

---

## Pendências

| # | Item | Bloqueio |
|---|---|---|
| 1 | Executar Dependency-Check completo | chave de API do NVD |
| 2 | Executar Trivy sobre a imagem | Trivy não instalado |
| 3 | Revisar a fatia `features` | sem testes; fora da meta de cobertura |
| 4 | Tornar a trilha *append-only* no banco | exige revogar `UPDATE`/`DELETE` do usuário da aplicação |
