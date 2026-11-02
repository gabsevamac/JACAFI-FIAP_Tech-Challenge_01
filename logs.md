**Proteção CSRF Desativada (Alta Severidade)**

* **Problema:** A proteção contra *Cross-Site Request Forgery* (CSRF) do Spring Security foi desabilitada no arquivo `SecurityConfig.java`.


* **Solução:** Reative a proteção CSRF caso a aplicação utilize sessões tradicionais baseadas em cookies. Se a API for estritamente *stateless* (utilizando tokens JWT, por exemplo), a desativação pode ser uma configuração esperada, mas deve ser justificada documentando a decisão ou suprimindo o alerta através de anotações como `@SuppressFBWarnings` se for um falso positivo de segurança.

**Exposição de Representação Interna - EI_EXPOSE_REP / EI_EXPOSE_REP2 (Média Severidade)**

* **Problema:** Diversas classes, incluindo `InventoryController`, `InventoryItemResponse`, `PageParameters` e vários serviços de aplicação (`RegisterInventoryItemService`, etc.), armazenam ou retornam instâncias de objetos mutáveis (como listas e datas) diretamente.


* **Solução:** Implemente cópias defensivas no código. Ao invés de armazenar a referência direta passada no construtor ou método (ex: `this.lista = lista`), utilize `List.copyOf(lista)` ou clone os objetos. Ao retornar esses valores, forneça representações imutáveis usando `Collections.unmodifiableList()` ou retorne cópias exatas dos objetos originais.

**Vulnerabilidade a Ataques de Finalizer - CT_CONSTRUCTOR_THROW (Média Severidade)**

* **Problema:** Exceções podem ser lançadas durante a execução dos construtores nas classes `ServiceOrderStatusNotificationProcessor`, `ResendStatusEmailSender`, `EventOutboxPublisher`, `BusinessException`, entre outras. Isso permite que a instância permaneça parcialmente inicializada, abrindo margem para ataques via métodos `finalize()`.


* **Solução:** Torne essas classes finais (adicionando a palavra-chave `final`) para impedir que subclasses sobrescrevam e abusem do estado parcialmente construído. Alternativamente, remova a lógica que lança exceções dos construtores, movendo a validação para métodos de fábrica estáticos (Static Factory Methods).

**Cross-Site Scripting (XSS) no Servlet (Média Severidade)**

* **Problema:** O uso do método `java.io.PrintWriter.write(String)` no tratador `SecurityProblemDetailHandler.java` está diretamente vulnerável à injeção de scripts maliciosos.


* **Solução:** Sanitize ou codifique os dados inseridos (HTML/JSON Encoding) antes de escrevê-los na resposta. Para padronizar a saída de forma segura, prefira utilizar serializadores JSON testados, como o `ObjectMapper` do Jackson, em vez de manipular o buffer `PrintWriter` manualmente.

**Modificação de String Após Validação (Baixa Severidade)**

* **Problema:** No arquivo `TaxId.java`, um valor textual está sendo alterado depois de já ter passado pelas etapas de validação. Invasores podem explorar isso passando strings estrategicamente construídas que contornam as verificações de segurança antes de serem modificadas para seu estado malicioso.


* **Solução:** Inverta a ordem das operações estruturais: aplique processos de limpeza, formatação de caracteres ou normalização de espaços *exclusivamente antes* de executar qualquer lógica ou função de validação de dados.

**Injeção de CRLF em Logs (Baixa Severidade)**

* **Problema:** As chamadas para os métodos `info`, `warn` e `error` da biblioteca SLF4J em classes como `GlobalExceptionHandler` e `LogSafe` recebem argumentos dinâmicos que podem conter caracteres maliciosos de quebra de linha (`\r` e `\n`). Isso permite a falsificação de entradas de log.


* **Solução:** Configure a biblioteca responsável pela saída dos logs (como Logback ou Log4j2) para codificar as mensagens de forma segura contra quebras de linha usando parâmetros como `%replace(%msg){'[\r\n]+', ''}` no padrão do console, ou sanitize ativamente as variáveis locais que representam erros antes de enviá-las para a API de log.

**Tratamento Inadequado de Unicode (Baixa Severidade)**

* **Problema:** As classes `KeycloakJwtAuthenticationConverter` e `SortableFields` falham ao normalizar ou mapear corretamente caracteres Unicode.


* **Solução:** Utilize a classe `java.text.Normalizer` para transformar as strings para um formato padrão, como `Normalizer.normalize(input, Normalizer.Form.NFKC)`, antes de conduzir qualquer validação de *regex* ou comparações com literais do sistema.

**Uso de Cláusula Throws Genérica (Baixa Severidade)**

* **Problema:** Métodos em `SecurityConfig.java` declaram lançar a exceção genérica raiz (`throws Exception`) na sua assinatura.


* **Solução:** Substitua a cláusula ampla e genérica pelas exceções específicas e exatas que podem efetivamente ser disparadas durante a execução dos métodos no escopo do arquivo.

**Identificação de Endpoints do Spring (Informativo)**

* **Problema:** Múltiplas ocorrências de `SPRING_ENDPOINT` foram registradas em controladores focados em domínios da aplicação (`CustomerController`, `InventoryController`, `LaborOperationController`, `ServiceOrderController` e `VehicleController`).


* **Solução:** Este não é um erro de código, mas um marcador gerado pela extensão FindSecBugs com fins de mapeamento de superfície. Assegure-se de que os métodos expostos naquelas linhas estão adequadamente configurados nas regras de autorização de rede e requisições HTTP da aplicação global. Nenhuma modificação em nível de arquivo Java local costuma ser necessária para esta tag.