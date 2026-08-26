# 0005 — Formato único de mascaramento de dados pessoais

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Placa de veículo e CPF/CNPJ são dados pessoais sob o Art. 5º, I da LGPD — **não** dados
sensíveis, que são a categoria fechada do Art. 5º, II. O Art. 6º, VII os mantém fora de
log, mensagem de erro e stack trace.

Duas fatias já mascaravam, cada uma ao seu modo:

| | `vehicle` | `customer` |
|---|---|---|
| Formato | `ABC***3` | `529***25` |
| Revela | 4 de 7 | 5 de 11 |

O resultado é um log onde a mesma pessoa aparece de duas formas, o que anula justamente a
correlação que a máscara parcial existe para permitir.

## Decisão

`shared/lgpd/Masker` passa a ser a **única fonte do formato**, e as fatias delegam a ele.

| Método | Entrada | Saída | Por que essa ponta |
|---|---|---|---|
| `licensePlate` | `ABC1D23` | `ABC****` | As três primeiras letras são comuns aos dois layouts brasileiros, então a máscara não revela se a placa é Mercosul ou antiga |
| `document` | `52998224725` | `********725` | Os dígitos iniciais do CPF correlacionam com a região emissora; os finais são verificadores, derivados do resto |
| `email` | `mariana@example.com` | `m***@example.com` | O domínio identifica organização, não pessoa |

O formato adotado é **mais restritivo** que o anterior: revela três caracteres em vez de
quatro ou cinco.

Regras que o tipo impõe:

- **Nada lança exceção.** Um utilitário de mascaramento que pode falhar acaba embrulhado em
  `try/catch` e eventualmente pulado — e o modo de falha do "pulado" é o valor íntegro no
  log.
- **Valor abaixo de seis caracteres é mascarado por inteiro.** Manter três esconderia menos
  do que revela.
- **O run de asteriscos é limitado a onze.** A máscara é proporcional, o que preserva o
  comprimento — inofensivo para CPF e CNPJ, que já se distinguem por ele. Mas o token que
  substitui a placa na remoção tem 41 caracteres, e reproduzir esse comprimento gerava uma
  linha de log com 38 asteriscos.

## Alternativas consideradas

**Adotar o formato já implementado.** Nenhum teste mudaria, nenhum comportamento mudaria.
Recusado por revelar mais do que o necessário: a máscara mais restritiva serve à mesma
correlação.

**Cada fatia mantém o seu.** É o estado que gerou o problema.

**Máscara total.** Elimina a correlação entre duas linhas de log sobre o mesmo veículo, que
é o que torna uma investigação possível.

## Consequências

- Sete asserções de formato foram atualizadas em cinco arquivos de teste de duas fatias.
- Os testes existentes acharam dois defeitos na primeira versão do `Masker`: um valor de
  quatro caracteres virava `ABC*` (revelando 75%), e o token de remoção virava 38
  asteriscos. Ambos corrigidos pelo limiar e pelo teto acima.
- **O que a máscara não cobre:** `vehicles.license_plate` guarda a placa íntegra, a API a
  devolve íntegra para chamador autenticado, e a trilha de auditoria também — ver
  [0009](0009-trilha-de-auditoria-com-valores-integros.md). Mascaramento é regra de
  *log e mensagem de erro*, não de armazenamento.
