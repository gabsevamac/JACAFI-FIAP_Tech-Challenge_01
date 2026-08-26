# 0015 — OWASP Dependency-Check para as dependências, em perfil separado

**Status:** Aceito
**Data:** 2026-08-26

## Contexto

A segunda frente do relatório de vulnerabilidades é o que **importamos**. Esta aplicação
traz Spring Boot 4, Hibernate 7, springdoc, jjwt, driver do Postgres e as transitivas de
todos eles — muito mais código de terceiros do que código nosso, e é ali que mora a maioria
dos CVEs de um sistema Java típico.

O SAST de [0014](0014-spotbugs-e-findsecbugs-para-sast.md) não enxerga isso: ele analisa o
nosso bytecode, não a lista de versões contra uma base de vulnerabilidades conhecidas.

## Decisão

**OWASP Dependency-Check 13.0.0**, num **perfil `security-scan` separado**, fora do build
padrão:

```bash
mvn -Psecurity-scan verify -Dnvd.api.key=SUA_CHAVE
```

Fora do `mvn verify` de todo dia por dois motivos concretos: o plugin mantém uma cópia local
da base do NVD, e a primeira execução leva dezenas de minutos; e desde 2023 o NVD exige
chave de API para não aplicar limite de taxa severo. Um scan assim no ciclo de feedback
torna o build inutilizável e o faz depender de um serviço externo estar no ar.

`failBuildOnCVSS=7` reprova em vulnerabilidade alta ou crítica. O limiar não é zero porque
uma base de CVE tem falso positivo e transitividade que não se remove — e um portão que
reprova sempre é um portão que alguém desliga.

Supressões ficam em `config/dependency-check-suppressions.xml`, cada uma exigindo motivo e
data de reavaliação. Supressão sem prazo vira permissão permanente por inércia, e o
relatório passa a afirmar que não há vulnerabilidade quando o que há é uma lista de coisas
que decidimos não olhar.

## Alternativas consideradas

**Trivy também para dependências, em vez de duas ferramentas.** É a alternativa mais forte,
e a recusa é parcial. O Trivy varre `pom.xml` e JARs além de imagem, é ordens de grandeza
mais rápido, e **não exige chave do NVD** — usaria uma ferramenta só para as duas frentes.
O que o Dependency-Check dá em troca é o formato de relatório que a banca reconhece e o
selo OWASP, que num trabalho avaliado tem peso. **Se o grupo preferir simplificar, trocar
por Trivy é defensável e este ADR deve ser substituído.**

**Snyk.** Melhor experiência das três e boa base própria. Recusado por exigir conta e por
o plano gratuito ter limite de varreduras — dependência de fornecedor num trabalho que
precisa continuar rodando depois da entrega.

**Dependabot.** Nativo do GitHub, gratuito, e o repositório já está lá. Recusado porque
**não é portão**: ele abre PR e emite alerta, mas não reprova build. Complementar, não
substituto — vale ligar de qualquer forma.

**Não varrer dependências.** Deixaria fora justamente onde está a maior parte do risco.

## Consequências

- **Ainda não foi executado.** Falta a chave do NVD. Isso está declarado no relatório como
  pendência, e não omitido — um relatório que silencia sobre o que não rodou é pior do que
  um que não rodou.
- `skipTestScope=true`: dependência de teste não vai para produção, e um CVE no container
  de teste não é superfície de ataque da aplicação entregue.
- O arquivo de supressões nasce **vazio**, de propósito. Preencher preventivamente seria
  suprimir achado que ninguém viu.
- Duas ferramentas significam dois relatórios para ler e dois conjuntos de versões para
  manter.
