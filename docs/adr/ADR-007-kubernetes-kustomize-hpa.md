# ADR-007 — Orquestração Kubernetes com Kustomize, Componentes e HPA

## Status

Aceito.

## Contexto

O projeto precisa executar em três cenários distintos de Kubernetes sem duplicar manifestos:

1. **Desenvolvimento local (`local`):** cluster local (Docker Desktop) com uma única réplica da API, banco PostgreSQL em container dentro do próprio cluster com volume persistente (`PersistentVolumeClaim`) e estratégia de substituição simples (`Recreate`).
2. **Teste de carga isolado (`load-test`):** ambiente local em namespace dedicado (`jacafi-load-test`), expondo `NodePorts` para geradores de carga (k6) e ativando o *Horizontal Pod Autoscaler* (HPA) com janela curta de redução para demonstração.
3. **Nuvem AWS (`eks`):** cluster EKS consumindo o banco gerenciado RDS com validação estrita de certificado TLS (`sslmode=verify-full`), expondo `NodePorts` fixos para os *Target Groups* do ALB, utilizando a imagem publicada no ECR e mantendo o HPA ativo com *RollingUpdate*.

## Alternativas consideradas

- **Manter manifestos YAML independentes por ambiente:** simples no início, mas qualquer alteração em probes, *securityContext*, portas ou variáveis de ambiente da API precisa ser replicada manualmente em três lugares, gerando deriva de configuração.
- **Empacotar a aplicação como um Helm Chart:** poderoso para distribuição de pacotes de terceiros, mas introduz templates Go e valores condicionais que reduzem a legibilidade direta dos manifestos YAML e exigem binário adicional na esteira, enquanto o `kubectl` já possui o Kustomize integrado nativamente (`kubectl apply -k`).

## Decisão

### 1. Estrutura em `base`, `components` e `overlays` com Kustomize nativo

Os manifestos em [k8s/](../../k8s/README.md) são organizados sem duplicação:

- **`k8s/base`:** define o `Namespace`, o `ConfigMap` base, o `Deployment` e o `Service` da API com endurecimento de segurança (*non-root* UID `10001`, *filesystem* somente leitura, remoção de *capabilities* Linux, seccomp `RuntimeDefault`, probes de *startup*, *readiness* e *liveness*, e `initContainer` que aguarda o PostgreSQL via `pg_isready`).
- **`k8s/components/keycloak`:** componente reutilizável com o `Deployment` e o `Service` do Keycloak importando o realm versionado.
- **`k8s/components/autoscaling`:** componente opcional que adiciona o `HorizontalPodAutoscaler` (`1` a `3` réplicas, metas de 70% de CPU sobre o *request* de `250m` e `800 MiB` de memória média), altera a estratégia do `Deployment` da API para `RollingUpdate` (`maxSurge: 1`, `maxUnavailable: 0`) e remove o campo estático `spec.replicas` para evitar conflito com o controlador do HPA.
- **`k8s/overlays/{local,load-test,eks}`:** aplicam apenas os *patches* específicos de cada ambiente (banco em container vs. RDS, `ClusterIP` vs. `NodePort`, endereços do Keycloak e parâmetros de estabilização do HPA).

### 2. Substituição explícita de placeholders dinâmicos no overlay `eks`

Como o endereço do ALB, o endpoint do RDS e a URL do ECR são conhecidos apenas após o `terraform apply`, o overlay `k8s/overlays/eks` utiliza variáveis explícitas com prefixo `${EKS_*}`. Durante o job `deploy`, o pipeline executa `envsubst` com lista restrita de variáveis antes de `kubectl kustomize` e falha imediatamente caso reste algum placeholder não resolvido (`grep -n '[$]{EKS_'`).

## Consequências

- **Fonte única da verdade para a API:** alterações de segurança, recursos ou probes em `k8s/base/api.yaml` propagam automaticamente para todos os ambientes.
- **Compatibilidade nativa com `kubectl`:** nenhuma ferramenta externa além de `kubectl` e `envsubst` é necessária para renderizar e validar os manifestos (`kubectl kustomize`).
- **Calibração realista de memória na JVM:** o alvo de memória do HPA foi definido em `800 MiB` (valor absoluto acima do *request* de `512 MiB` e abaixo do *limit* de `1 GiB`) porque o heap e o metaspace retidos pela JVM após o aquecimento mantêm o consumo basal próximo a 400–450 MiB; um alvo percentual baixo manteria o cluster permanentemente escalado em 3 réplicas mesmo ocioso.

