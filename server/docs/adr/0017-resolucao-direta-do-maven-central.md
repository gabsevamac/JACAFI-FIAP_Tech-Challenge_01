# 0017 — Resolver dependências direto do Maven Central

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

Um dos quatro integrantes tem, no `~/.m2/settings.xml` da sua máquina de trabalho, um mirror
corporativo declarado assim:

```xml
<mirror>
  <id>nexus-fiergs-central</id>
  <mirrorOf>*,!nexus-fiergs-releases,!nexus-fiergs-central,!nexus-fiergs-jaspersoft</mirrorOf>
  <url>https://srvnexus.sistemafiergs.com.br/repository/maven-central/</url>
</mirror>
```

O curinga `*` intercepta **todos** os repositórios, inclusive o `central` declarado pelo
parent do Spring Boot. O host exige VPN da empresa.

Consequência descoberta na prática, quando a VPN caiu no meio do trabalho: a resolução falha
com `Name or service not known` e o build para. E o mesmo vale, permanentemente, para os
**outros três integrantes**, que nunca tiveram acesso àquele host.

Um build que só funciona na rede de uma empresa específica não é reprodutível — e este é um
trabalho acadêmico entregue a uma banca que também não tem essa VPN.

## Decisão

O projeto passa a declarar a própria resolução, em `server/.mvn/settings.xml`, aplicada
automaticamente por `server/.mvn/maven.config`:

```
-s
.mvn/settings.xml
```

Aponta direto para `https://repo.maven.apache.org/maven2`. Todos os artefatos deste projeto
são públicos, então não há credencial a preservar.

Verificado empiricamente: um artefato ausente do cache local foi baixado de
`repo.maven.apache.org`, sem passar pelo Nexus.

## Alternativas consideradas

**Cada um ajusta o seu `~/.m2/settings.xml`.** Recusado por dois motivos. Exige que o dono
da máquina de trabalho quebre a configuração corporativa dele, que existe por política da
empresa e serve aos projetos de trabalho dele. E é acordo verbal: some no próximo clone, na
próxima máquina, no próximo integrante.

**Manter o Nexus e pedir VPN.** Impossível para quem não é da empresa, banca inclusive.

**Declarar `<repositories>` no `pom`.** Não resolve: um mirror com `mirrorOf` curinga
intercepta repositório declarado no `pom` do mesmo jeito. Esse foi o primeiro caminho
tentado, e é por isso que a solução precisa passar por `settings.xml`.

**Commitar o `settings.xml` e documentar `mvn -s settings.xml`.** Funciona e depende de
alguém lembrar do parâmetro, o que é o mesmo tipo de acordo verbal acima. O
`.mvn/maven.config` remove o "lembrar".

## Consequências

- O build funciona igual nas quatro máquinas e na da banca.
- **`-s` substitui o `settings.xml` do usuário, não o complementa.** Se algum integrante
  depender de proxy ou credencial declarados no arquivo global, eles não valem aqui. Nenhum
  artefato deste projeto exige autenticação, mas alguém atrás de proxy corporativo
  obrigatório precisará declará-lo também no arquivo do projeto.
- O build precisa ser invocado **a partir de `server/`**. O caminho é relativo porque o
  Maven 3.9.16 não interpola `${maven.projectBasedir}` dentro do `maven.config` — o
  resultado é um erro com o literal no caminho. De outro diretório, passe
  `-s server/.mvn/settings.xml`.
- Perde-se o cache e o controle do Nexus corporativo. Irrelevante aqui: o projeto é pessoal
  e acadêmico, e não está sujeito a política de artefatos da empresa.
