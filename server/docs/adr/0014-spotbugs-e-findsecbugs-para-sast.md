# 0014 — SpotBugs com FindSecBugs para análise estática de segurança

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

O enunciado pede relatório de análise de vulnerabilidades. Uma das frentes é o código que
nós escrevemos — injeção, criptografia fraca, *path traversal*, XXE, segredo embutido,
*log forging*.

Restrições do contexto: é trabalho acadêmico de quatro pessoas, sem infraestrutura própria,
e a ferramenta precisa rodar na máquina de cada um e no `mvn verify` sem depender de
serviço externo ou de conta.

## Decisão

**SpotBugs 4.10.4 com o plugin FindSecBugs 1.14.0**, no build padrão.

O SpotBugs sozinho procura defeito genérico; o FindSecBugs acrescenta os detectores de
segurança. Roda offline, é plugin Maven nativo, não exige servidor nem cadastro.

Configurado com `effort=Max` e `threshold=Low` — a combinação mais ruidosa **de propósito**.
A triagem deve ser nossa, documentada em `docs/analise-vulnerabilidades.md`, e não da
ferramenta: um limiar alto esconde achado de baixa prioridade que é procedente no nosso
contexto, e foi exatamente o caso do `CRLF_INJECTION_LOGS`, rank 15, que era real.

Código de teste fica fora: um segredo embutido num teste é um valor fixo de teste, e o ruído
afogaria os achados de produção.

## Alternativas consideradas

**SonarQube.** Melhor ferramenta das listadas, e a que o grupo mais provavelmente encontrará
no mercado. Recusada por infraestrutura: exige servidor rodando, e um trabalho acadêmico não
tem onde hospedá-lo de forma que os quatro integrantes e a banca alcancem. O JaCoCo já
gera XML no formato que o Sonar lê, então adotá-lo depois não custa retrabalho.

**CodeQL.** Análise mais profunda que a do SpotBugs, com rastreamento de fluxo de dados
entre métodos. Viável, porque o repositório está no GitHub. Recusada por não rodar
localmente com facilidade: o achado aparece no PR, e não no `mvn verify` de quem está
escrevendo o código. Fica como candidata natural se o grupo montar CI.

**PMD.** Foco em estilo e defeito comum; o conjunto de regras de segurança é bem mais fraco
que o do FindSecBugs.

**Semgrep.** Boa ferramenta e regras legíveis, mas o catálogo para Java é menos maduro que
para linguagens de script, e a operação confortável passa pelo serviço deles.

## Consequências

- 46 achados na primeira execução, triados um a um no relatório. **29 de categoria
  SECURITY**, dos quais um exigiu correção de código (`LogSafe`), um foi aceito com
  justificativa arquitetural (CSRF desabilitado em API stateless) e o resto é falso positivo
  ou informativo.
- **O build ainda não reprova por achado do SpotBugs.** A configuração roda a análise e
  gera o relatório; ligar o `check` exige antes zerar ou suprimir os 46, e suprimir em
  massa para "ficar verde" produziria exatamente o relatório sem valor que este documento
  tenta evitar.
- Analisa **bytecode**, não código-fonte. Não vê o que o compilador apaga — constante
  interpolada, comentário, anotação com retenção de fonte.
- A ferramenta não tem detector para os controles mais importantes deste projeto: não vazar
  dado pessoal em erro, não distinguir usuário inexistente no 401, mascarar placa em log.
  Esses são provados por **teste** (`ErrorLeakageIT`, `LogMaskingTest`), o que é garantia
  mais forte do que um scan, porque não depende de a ferramenta ter pensado no caso.
