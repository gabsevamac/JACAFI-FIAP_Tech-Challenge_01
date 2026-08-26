# 0013 — JaCoCo para medir cobertura, com dois agentes

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

O enunciado exige cobertura mínima de 80% nos domínios críticos, e exige **artefato** que
comprove — sem relatório, a afirmação não vale nada.

Uma restrição do nosso build governa a escolha: os testes estão divididos entre Surefire
(`*Test`) e Failsafe (`*IT`), em **JVMs diferentes**. Os 47 testes de integração são 19% do
total e cobrem justamente a infraestrutura que os unitários não alcançam. Uma ferramenta que
só instrumente uma das JVMs mede a metade errada.

## Decisão

**JaCoCo 0.8.15**, com **duas execuções de preparação**: `prepare-agent` para a JVM do
Surefire e `prepare-agent-integration` para a do Failsafe, cada uma gravando o seu arquivo
e exportando o `argLine` sob nome próprio (`surefireArgLine`, `failsafeArgLine`). Os dois
arquivos são mesclados num relatório só.

Deixar as duas no `argLine` default faria a segunda sobrescrever a primeira. Usar só a
primeira faria os testes `*IT` não contarem — silenciosamente, com um número menor e
plausível.

O limiar de 80% vale para `*/domain` e `*/application`, e não para o projeto inteiro: é
onde estão as regras de negócio. Controller, entidade JPA e mapper são código de ligação, e
perseguir cobertura neles produz teste que confirma que o framework funciona.

## Alternativas consideradas

**Cobertura.** Sem release desde 2015 e sem suporte real a bytecode moderno. Não roda em
Java 25.

**OpenClover.** Mantido, e mede mais do que o JaCoCo — cobertura por teste, por exemplo.
Recusado por integração: o formato do JaCoCo é o que SonarQube, GitHub e IDEs leem sem
configuração, e este projeto tem quatro pessoas com ferramentas diferentes.

**Cobertura da IDE.** Não é portão de build. Um número que só existe na máquina de quem
abriu a IDE não comprova nada para a entrega.

**PIT (teste de mutação), em vez de cobertura de linha.** Esta é a alternativa séria, e a
recusa é por escopo, não por mérito. Cobertura de linha afirma que a linha **executou**, não
que alguém **verificou** o resultado — um teste sem asserção nenhuma dá 100%. Teste de
mutação altera o código e exige que algum teste falhe, o que mede verificação de verdade.

Durante este trabalho, mutações **manuais** encontraram três testes que passavam sem provar
nada: o round-trip de `Instant`, o desempate de paginação e a própria regra do JaCoCo.
Automatizar isso com PIT tem valor real, e fica registrado como próximo passo — mas o
enunciado cobra cobertura, e entregar mutação sem cobertura seria não entregar o pedido.

## Consequências

- `mvn verify` reprova se `*/domain` ou `*/application` caírem abaixo de 80%.
- Relatório HTML em `target/site/jacoco/`, e XML pronto para SonarQube se o grupo quiser.
- O `maven-surefire-plugin` precisou ser declarado no `pom` só para receber o `argLine`. Sem
  isso o agente não é anexado e os unitários não instrumentam nada.
- **A fatia `features` está excluída da regra**, com 4,8% e 15,2%. Ver o comentário no `pom`:
  é dívida registrada, não dispensa.
- O número medido não distingue linha executada de linha verificada. Enquanto PIT não
  entrar, 80% aqui significa menos do que parece — e vale dizer isso à banca antes que ela
  pergunte.
