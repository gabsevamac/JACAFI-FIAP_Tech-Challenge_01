# Avisos de terceiros

Este projeto é distribuído sob a licença MIT (ver `LICENSE`). Os componentes abaixo foram
incorporados ao código-fonte — não como dependência declarada, mas copiados e adaptados — e
mantêm os termos das suas origens.

## Caelum Stella — validação de dígito verificador de CPF

- **Origem:** [`CPFValidator.java`](https://github.com/caelum/caelum-stella/blob/master/stella-core/src/main/java/br/com/caelum/stella/validation/CPFValidator.java), versão 2.2.2
- **Autoria:** Leonardo Bessa (`@author` no original)
- **Copyright:** Copyright Caelum
- **Licença:** Apache License 2.0 — texto integral em [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt),
  e o aviso do projeto de origem em [`licenses/caelum-stella-NOTICE.txt`](licenses/caelum-stella-NOTICE.txt)
- **Onde está:** classe `CpfValidator`, em
  `server/src/main/java/com/jacafi/tech/customer/entity/Cpf.java`

**Modificações em relação ao original** (declaradas conforme a seção 4b da Apache 2.0):

1. Removidas as dependências de framework do Stella: `DigitoPara`, `DigitoGenerator`,
   `MessageProducer`, `SimpleMessageProducer`, `ValidationMessage`, `CPFFormatter` e `CPFError`.
   O cálculo do dígito, que o original expressava pelo builder `DigitoPara`, foi escrito de forma
   explícita — a aritmética é a mesma, e o javadoc da classe registra a correspondência.
2. `CPFFormatter.unformat` lançava `IllegalArgumentException` para entrada não reconhecida; aqui o
   método equivalente devolve `null`.
3. A classe passou a ser `final` e package-private, e os métodos estáticos; o original é público e
   instanciável com configuração.
4. A validação em si — ordem das verificações, pesos, critério de aceitação e a rejeição de
   sequências de dígitos repetidos — **não foi alterada**.

## Receita Federal — validação de dígito verificador de CNPJ alfanumérico

- **Origem:** [Códigos e documentos técnicos do CNPJ](https://www.gov.br/receitafederal/pt-br/centrais-de-conteudo/publicacoes/documentos-tecnicos/cnpj/codigos-cnpj.zip/view),
  Receita Federal do Brasil
- **Onde está:** classe `CnpjValidator`, em
  `server/src/main/java/com/jacafi/tech/customer/entity/Cnpj.java`
- **Licença:** os termos de uso do pacote publicado pela Receita Federal **não foram verificados**.
  Trata-se de implementação de referência publicada por órgão público para adoção pelo mercado, mas
  isso não é o mesmo que uma licença declarada. Pendente de conferência antes de qualquer
  distribuição fora do contexto acadêmico.

**Modificações em relação ao original:**

1. A `IllegalArgumentException` de `calculaDV` não interpola mais o CNPJ na mensagem. Mensagem de
   exceção alcança log, e CNPJ é dado pessoal (LGPD Art. 5º I, Art. 6º VII).
2. A mensagem passou para o inglês, conforme a convenção de idioma do projeto.
3. A classe passou a ser `final` e package-private, com construtor privado.
4. Nomes de identificadores, estrutura e aritmética — incluindo os pesos e o tratamento
   alfanumérico — **não foram alterados**. Os nomes em português são parte da procedência.
