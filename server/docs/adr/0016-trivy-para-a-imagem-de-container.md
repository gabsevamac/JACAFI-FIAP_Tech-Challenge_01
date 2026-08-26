# 0016 — Trivy para varrer a imagem de container

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

A terceira frente é a imagem que entregamos. O `Dockerfile` parte de
`eclipse-temurin:25-jre`, e a imagem final carrega uma distribuição de sistema operacional
inteira: OpenSSL, glibc, zlib, utilitários. Nada disso aparece no `pom`, então nem o SAST de
[0014](0014-spotbugs-e-findsecbugs-para-sast.md) nem o SCA de
[0015](0015-dependency-check-em-perfil-separado.md) enxergam.

É uma superfície real: a maioria dos CVEs de severidade alta numa imagem Java típica está
nos pacotes do sistema base, não no JAR.

## Decisão

**Trivy**, fora do Maven:

```bash
docker build -t sinates:scan ./server
trivy image --severity HIGH,CRITICAL sinates:scan
```

Fora do Maven porque a coisa varrida não é produzida pelo Maven — é produzida pelo
`docker build`. Um plugin que chamasse o Docker de dentro do `verify` acoplaria o build da
aplicação à presença de um *daemon* de container na máquina de quem só quer rodar teste.

Binário único, base de vulnerabilidades própria e sem chave de API, e varre também
`Dockerfile` e IaC caso o grupo precise depois.

## Alternativas consideradas

**Grype (Anchore).** Equivalente em qualidade e igualmente simples. Empate técnico; o Trivy
ganhou por já estar no repertório do grupo e por cobrir mais tipos de alvo com a mesma
ferramenta.

**Docker Scout.** Integrado ao Docker Desktop e conveniente, mas as funções úteis exigem
conta no Docker Hub, e o projeto não deve depender de cadastro de ninguém.

**Clair.** Exige servidor e banco. Desproporcional para um trabalho acadêmico.

**Não varrer a imagem.** Deixaria de fora a camada onde a maior parte dos CVEs altos
costuma estar, e o relatório afirmaria uma cobertura que não tem.

## Consequências

- **Não foi executado**: o Trivy não está instalado no ambiente de desenvolvimento. Consta
  como pendência explícita no relatório.
- A varredura fica fora do portão automático. Cabe ao grupo rodá-la antes da entrega — e
  isso é conhecidamente frágil, porque depende de alguém lembrar. Colocá-la em CI, quando
  houver, resolve.
- Provável ação de mitigação já previsível: fixar a imagem base por *digest* em vez de tag,
  para que `25-jre` não mude por baixo entre o build que foi varrido e o que foi entregue.
