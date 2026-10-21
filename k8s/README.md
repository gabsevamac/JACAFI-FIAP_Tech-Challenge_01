# Kubernetes local — Docker Desktop

Execute os comandos em PowerShell, na raiz do repositório. Requer Docker Desktop com containers Linux e Kubernetes habilitado, `docker`, `kubectl`, acesso às imagens PostgreSQL/Keycloak e uma StorageClass padrão capaz de provisionar o PVC de 2 GiB.

Este ambiente é de desenvolvimento: uma API, um PostgreSQL com volume local e um Keycloak `start-dev` com dados efêmeros. Não oferece alta disponibilidade. O PVC preserva o PostgreSQL quando o Pod é substituído, mas não é backup nem protege contra perda/reset do cluster local. A substituição do Pod do Keycloak perde usuários/alterações feitas no console, sessões e chaves de assinatura; obtenha novos tokens após a recriação. O [realm versionado](../keycloak/jacafi-realm.json) é a fonte da configuração local.

## Preparação e instalação

Confira o contexto antes de alterar o cluster. Os comandos seguintes pressupõem `docker-desktop`:

```powershell
kubectl config current-context
kubectl config use-context docker-desktop
kubectl get nodes
kubectl get storageclass
docker build -t jacafi/server:k8s-local ./server
kubectl apply -f k8s/namespace.yaml
```

Crie o arquivo local de credenciais uma única vez e substitua todos os valores `replace-with-...` no editor. O arquivo `k8s/secrets.env` é ignorado pelo Git; não versione credenciais nem salve o YAML gerado do Secret. Mantenha `RESEND_API_KEY` vazio se não usar e-mail.

```powershell
if (-not (Test-Path k8s/secrets.env)) {
    Copy-Item k8s/secrets.env.example k8s/secrets.env
}
notepad k8s/secrets.env
```

Depois de salvar e fechar o editor, crie o Secret e importe o JSON existente como ConfigMap. O pipeline envia o YAML diretamente ao Kubernetes:

```powershell
kubectl -n jacafi create secret generic jacafi-secrets --from-env-file=k8s/secrets.env --dry-run=client -o yaml | kubectl apply -f -
kubectl -n jacafi create configmap keycloak-realm --from-file=jacafi-realm.json=keycloak/jacafi-realm.json --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -k k8s
kubectl -n jacafi rollout status deployment/database --timeout=300s
kubectl -n jacafi rollout status deployment/keycloak --timeout=300s
kubectl -n jacafi rollout status deployment/api --timeout=300s
kubectl -n jacafi get pods,services,pvc
```

O Kustomize aplica Namespace, configuração pública, Deployments, Services e PVC; não cria o Secret nem o ConfigMap do realm e não habilita HPA. A API aguarda o banco e executa as migrações Flyway na inicialização. Os Services são internos (`ClusterIP`).

Se a API apresentar `ImagePullBackOff`/`ErrImagePull`, confira `kubectl -n jacafi describe pod <nome-do-pod>` e se o build usou o Docker Engine do Desktop. A disponibilidade de imagens locais varia com o provisionador (kubeadm ou kind) e o armazenamento de imagens do Desktop. Com kind, pode ser necessário carregar a imagem nos nós ou usar um registry acessível; verifique a configuração efetiva antes de assumir que a imagem construída está disponível. Caso seu cluster seja gerenciável pelo CLI `kind`, use `kind get clusters` para identificar o nome e `kind load docker-image jacafi/server:k8s-local --name <nome-do-cluster>`. Não presuma que o nome do contexto seja o nome do cluster kind.

## Acesso e smoke test

Libere as portas 8081 e 8082 caso o Compose esteja em execução. Em dois terminais separados, mantenha os encaminhamentos ativos:

```powershell
kubectl -n jacafi port-forward service/api 8082:8082
```

```powershell
kubectl -n jacafi port-forward service/keycloak 8081:8080
```

Em um terceiro terminal, verifique a saúde e obtenha um token do usuário de desenvolvimento presente no realm:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health/liveness
Invoke-RestMethod http://localhost:8082/actuator/health/readiness
$tokenResponse = Invoke-RestMethod -Method Post -Uri http://localhost:8081/realms/jacafi/protocol/openid-connect/token -ContentType 'application/x-www-form-urlencoded' -Body @{
    client_id = 'jacafi-web'
    grant_type = 'password'
    username = 'dev-employee'
    password = 'Dev-employee-2026'
}
Invoke-RestMethod -Uri http://localhost:8082/api/v1/customers -Headers @{ Authorization = "Bearer $($tokenResponse.access_token)" }
```

Espere `UP` nas duas sondas e uma resposta paginada de clientes (possivelmente vazia) na API autenticada. Essas credenciais e o password grant servem somente para desenvolvimento. O Swagger fica em `http://localhost:8082/swagger-ui/index.html`; o console do Keycloak, em `http://localhost:8081`, usa as credenciais administrativas do arquivo local.

O issuer público é `http://localhost:8081/realms/jacafi`; a API busca as chaves por `http://keycloak:8080/realms/jacafi/protocol/openid-connect/certs`. A diferença é intencional. Ao mudar a porta pública ou hostname do Keycloak, ajuste também `KC_HOSTNAME` em `keycloak.yaml` e `KEYCLOAK_ISSUER_URI` em `configmap.yaml`. Reinicie o port-forward se o Pod encaminhado for substituído.

## Atualizações e diagnóstico

Para mudar configuração pública, edite `k8s/configmap.yaml`, reaplique e reinicie os consumidores afetados: variáveis de ambiente não são atualizadas em Pods existentes.

```powershell
kubectl apply -k k8s
kubectl -n jacafi rollout restart deployment/api
kubectl -n jacafi rollout status deployment/api --timeout=300s
```

Para atualizar um Secret, edite `k8s/secrets.env`, repita o comando `create secret ... --dry-run=client ... | kubectl apply -f -` da instalação e reinicie os Deployments afetados. Alterar `DB_USERNAME`, `DB_PASSWORD` ou `DB_NAME` não altera usuários, senhas ou bancos já inicializados no PVC do PostgreSQL. Planeje a alteração correspondente no banco antes de atualizar os clientes; reiniciar o PostgreSQL não refaz essa inicialização. Não apague o PVC para rotacionar credenciais.

Para alterar o realm local, edite o JSON versionado, repita a criação do ConfigMap `keycloak-realm` e execute `kubectl -n jacafi rollout restart deployment/keycloak`. A recriação importa o realm e perde o estado efêmero descrito acima. Para e-mails, configure `RESEND_ENABLED`/`RESEND_FROM` no ConfigMap e a chave no Secret, usando remetente verificado.

Após reconstruir a imagem com a mesma tag, disponibilize-a novamente para os nós e reinicie `deployment/api`. Para evitar ambiguidades de cache em outros ambientes, use tags imutáveis ou digests.

```powershell
kubectl -n jacafi get events --sort-by=.lastTimestamp
kubectl -n jacafi describe deployment api
kubectl -n jacafi logs deployment/api --all-containers=true --tail=100
kubectl -n jacafi logs deployment/database --tail=100
kubectl -n jacafi logs deployment/keycloak --tail=100
```

Um PVC `Pending` exige verificar provisionamento e StorageClass. Readiness da API inclui conexão ao banco; não comprova emissão de tokens nem entrega de e-mail. O smoke test autenticado verifica também a integração com o Keycloak.

## HPA opcional — bloqueado por concorrência da outbox

`autoscaling.yaml` fica fora do Kustomize padrão. Define CPU média alvo de 70% dos requests, mínimo de 1 e máximo de 3 réplicas da API. **Não habilite no estado atual da aplicação:** cada réplica executa os schedulers da outbox, e a busca de eventos não faz claim/locking entre consumidores. Múltiplas réplicas podem processar o mesmo evento. Corrija e teste a coordenação/idempotência antes de habilitar o HPA ou aumentar réplicas manualmente. Por esse motivo, o padrão usa uma réplica e estratégia `Recreate`, com indisponibilidade durante atualização.

O HPA também requer Metrics Server funcional e requests de CPU nos containers (já definidos na API). A instalação/configuração do addon é responsabilidade do cluster; este conjunto não instala addons. Verifique compatibilidade, aggregation layer, autenticação/autorização do kubelet, certificados e conectividade conforme a [documentação do Metrics Server](https://kubernetes-sigs.github.io/metrics-server/). Confira as métricas antes de habilitar:

```powershell
kubectl get apiservice v1beta1.metrics.k8s.io
kubectl top nodes
kubectl -n jacafi top pods
```

Somente depois da correção da outbox e das métricas disponíveis:

```powershell
kubectl apply -f k8s/autoscaling.yaml
kubectl -n jacafi get hpa api
kubectl -n jacafi describe hpa api
```

Consulte o [funcionamento do Horizontal Pod Autoscaler](https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/). Métricas `<unknown>` indicam um problema a diagnosticar, não validação do escalonamento. Enquanto o HPA estiver ativo, reaplicar `replicas: 1` do manifesto pode redefinir temporariamente a escala; remova esse campo da configuração usada por um ambiente com HPA após liberar concorrência. Para desabilitar:

```powershell
kubectl -n jacafi delete hpa api --ignore-not-found
kubectl -n jacafi scale deployment/api --replicas=1
```

## Encerramento e dados

Para parar os workloads mantendo PVC, Secret e configurações, encerre os port-forwards com Ctrl+C, remova o HPA se tiver sido habilitado e reduza os Deployments a zero:

```powershell
kubectl -n jacafi delete hpa api --ignore-not-found
kubectl -n jacafi scale deployment/api deployment/keycloak deployment/database --replicas=0
```

Para retomar, execute `kubectl apply -k k8s` e repita os waits e port-forwards. O PostgreSQL reutiliza os dados; o Keycloak reimporta o realm.

**A exclusão do namespace remove também o PVC e pode apagar definitivamente os dados do volume**, conforme a política de reclaim do provisionador. Faça backup antes. Execute apenas se deseja descartar todo o ambiente:

```powershell
kubectl delete namespace jacafi
```

`kubectl delete -k k8s` também inclui Namespace e PVC: não use esse comando para uma pausa que deve preservar dados.

## Integração futura e validação

O CI atual gera um arquivo tar da imagem, mas não a publica em registry. Para um cluster remoto, integre publicação com tag imutável/digest, ajuste `image` no Deployment e configure `imagePullSecrets` quando necessário. Banco gerenciado, Keycloak persistente, TLS, backups e gestão externa de Secrets exigem configuração própria. Pipeline e Terraform não são implementados por estes manifests.

Para inspecionar o resultado sem executar workloads:

```powershell
kubectl kustomize k8s
kubectl apply --dry-run=server -k k8s
```

O dry-run dos recursos namespaced requer que `jacafi` já exista: o Namespace simulado não persiste durante esse comando. Renderização e validação pelo API server não comprovam subida dos containers, persistência, smoke test nem comportamento do HPA; execute as verificações deste guia no ambiente de destino.
