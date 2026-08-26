# 0001 — Formatador único, obrigatório no build

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Quatro integrantes editam fatias verticais em paralelo. Formatação divergente entre IDEs
já estava produzindo diff de ruído: um arquivo tocado por duas pessoas voltava com dezenas
de linhas alteradas sem mudança de comportamento, e o *code review* passava a discutir
indentação em vez de decisão de modelagem.

O repositório tinha as duas convenções misturadas — `TechApplication.java` com tabulação,
o resto com quatro espaços.

## Decisão

**Spotless com palantir-java-format**, ligado como `spotless:check` na fase `validate`.
Código fora do padrão reprova o build antes de compilar.

O `check` e não o `apply` no build é deliberado: um build que reescreve o código-fonte
sozinho altera o arquivo que a pessoa está editando no meio da edição. Formatar é
`mvn spotless:apply`, explícito.

A escolha do formatador foi **medida**, não presumida:

| Candidato | Roda no JDK 25? | Arquivos já limpos (de 89) |
|---|---|---|
| palantir-java-format 2.97.0 | sim | **16** |
| google-java-format 1.36.1 (AOSP) | sim | 9 |

Nenhum dos dois precisou das flags `add-exports` que a documentação mais antiga menciona.
O desempate foi o tamanho da reformatação inicial: menos arquivos tocados, menos autoria
perdida no `git blame`.

## Alternativas consideradas

**google-java-format em AOSP.** Funciona igualmente. Recusado só pelo desempate acima —
se houvesse preferência do grupo pelo estilo Google, a decisão se inverteria sem prejuízo.

**Convenção acordada, sem ferramenta.** É o estado que gerou o problema. Convenção depende
de quatro pessoas lembrarem, e a IDE de cada uma discorda por padrão.

**`spotless:apply` no build.** Removeria o passo manual, ao custo de o build modificar o
código-fonte durante a edição.

## Consequências

- Formatação deixa de ser assunto de revisão.
- Um commit de reformatação tocou 73 dos 89 arquivos. Sem tratamento, o `git blame`
  atribuiria o código de todos ao autor desse commit — por isso o `.git-blame-ignore-revs`,
  que **cada integrante precisa ativar uma vez** com
  `git config blame.ignoreRevsFile .git-blame-ignore-revs`.
- **Armadilha de versão registrada:** Spotless 2.44.5 estoura
  `NoSuchMethodError: Log$DeferredDiagnosticHandler.getDiagnostics()` no JDK 25, porque o
  javac mudou a assinatura interna. A 3.x acompanha. Não fazer downgrade.
- Quem tiver PR aberto durante o merge da reformatação terá conflito em quase toda linha.
