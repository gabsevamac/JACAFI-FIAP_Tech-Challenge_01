# Registros de Decisão de Arquitetura (ADR)

Formato conforme NYGARD, M. *Documenting Architecture Decisions*. Thoughtworks, 2011.

Um ADR registra uma decisão **com alternativa real**. Se não havia outro caminho
defensável, não é decisão: é consequência, e o lugar dela é o comentário no código.

Cada documento é imutável depois de aceito. Mudança de rumo entra como ADR novo que
declara qual substitui — reescrever o antigo apagaria o motivo pelo qual a decisão parecia
certa na época, que é justamente o que torna o registro útil.

## Índice

| # | Decisão | Status |
|---|---|---|
| [0001](0001-formatador-unico-obrigatorio-no-build.md) | Formatador único, obrigatório no build | Aceito |
| [0002](0002-checkstyle-restrito-a-imports.md) | Checkstyle restrito a três regras de import | Aceito |
| [0003](0003-utc-e-relogio-injetado.md) | UTC em todas as camadas e relógio injetado | Aceito |
| [0004](0004-precisao-de-tempo-em-microssegundos.md) | Precisão de tempo em microssegundos | Aceito |
| [0005](0005-formato-unico-de-mascaramento.md) | Formato único de mascaramento de dados pessoais | Aceito |
| [0006](0006-auditoria-tecnica-com-vocabulario-padronizado.md) | Auditoria técnica com vocabulário padronizado | Aceito |
| [0007](0007-remocao-logica-por-filtro-explicito.md) | Remoção lógica por filtro explícito | Aceito |
| [0008](0008-bloqueio-otimista-por-entidade-gerenciada.md) | Bloqueio otimista por cópia para entidade gerenciada | Aceito |
| [0009](0009-trilha-de-auditoria-com-valores-integros.md) | Trilha de auditoria com valores íntegros | Aceito |
| [0010](0010-contrato-proprio-de-paginacao.md) | Contrato próprio de paginação com lista branca | Aceito |
| [0011](0011-tratamento-de-erros-centralizado.md) | Tratamento de erros centralizado com traceId | Aceito |
| [0012](0012-permanecer-no-nivel-2-de-richardson-por-ora.md) | Permanecer no nível 2 de Richardson, por ora | Aceito |
| [0013](0013-jacoco-para-cobertura.md) | JaCoCo para cobertura, com dois agentes | Aceito |
| [0014](0014-spotbugs-e-findsecbugs-para-sast.md) | SpotBugs + FindSecBugs para SAST | Aceito |
| [0015](0015-dependency-check-em-perfil-separado.md) | Dependency-Check em perfil separado | Aceito |
| [0016](0016-trivy-para-a-imagem-de-container.md) | Trivy para a imagem de container | Aceito |
| [0017](0017-resolucao-direta-do-maven-central.md) | Resolução direta do Maven Central | Aceito |

## Modelo

```markdown
# NNNN — Título na forma de decisão tomada

**Status:** Proposto | Aceito | Substituído por [NNNN](...)
**Data:** AAAA-MM-DD

## Contexto
O que forçava uma escolha. Fatos, não preferências.

## Decisão
O que foi decidido, na voz ativa.

## Alternativas consideradas
Cada uma com o motivo da recusa. Uma alternativa sem motivo é enfeite.

## Consequências
O que passa a ser verdade — inclusive o que piorou.
```
