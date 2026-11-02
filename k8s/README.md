# Kubernetes — Docker Desktop, carga e AWS EKS

Os manifests compartilham a API em `base/` e separam infraestrutura por overlay:

| Overlay | Namespace | Banco / identidade | Acesso / réplicas |
|---|---|---|---|
| `overlays/local` | `jacafi` | PostgreSQL com PVC / Keycloak efêmero | Port-forward, uma API com `Recreate` |
| `overlays/load-test` | `jacafi-load-test` | PostgreSQL e Keycloak próprios, isolados do local | NodePort 30082/30081, HPA e `RollingUpdate` |
| `overlays/eks` | `jacafi` | RDS PostgreSQL (Terraform) / Keycloak efêmero no cluster | ALB do Terraform → NodePort 30080/30081, HPA e `RollingUpdate` |

`kubectl apply -k k8s` continua equivalente ao overlay local. Use contextos diferentes para local e EKS. Os overlays de carga/EKS incluem escalonamento, mas **a outbox ainda bloqueia uso concorrente com eventos de negócio**, conforme a seção de HPA. O Keycloak de `local` e `eks` vem do componente `components/keycloak`. O deploy no EKS é feito pelo pipeline, conforme a seção [AWS EKS](#aws-eks).

## Ambiente local

Execute os comandos em PowerShell, na raiz do repositório. Requer Docker Desktop com containers Linux e Kubernetes habilitado, `docker`, `kubectl`, acesso às imagens PostgreSQL/Keycloak e uma StorageClass padrão capaz de provisionar o PVC de 2 GiB.

Este ambiente é de desenvolvimento: uma API, um PostgreSQL com volume local e um Keycloak `start-dev` com dados efêmeros. Não oferece alta disponibilidade. O PVC preserva o PostgreSQL quando o Pod é substituído, mas não é backup nem protege contra perda/reset do cluster local. A substituição do Pod do Keycloak perde usuários/alterações feitas no console, sessões e chaves de assinatura; obtenha novos tokens após a recriação. O [realm versionado](../keycloak/jacafi-realm.json) é a fonte da configuração local.

## Preparação e instalação

### 1. Conferir o cluster e construir a imagem

Confira o contexto antes de alterar o cluster. Os comandos seguintes pressupõem `docker-desktop`:

```powershell
kubectl config current-context
kubectl config use-context docker-desktop
if ($LASTEXITCODE -ne 0 -or (kubectl config current-context) -ne 'docker-desktop') { throw 'Contexto local não selecionado.' }
kubectl get nodes
kubectl get storageclass
docker build -t jacafi/server:k8s-local ./server
kubectl apply -f k8s/base/namespace.yaml
```

O nó deve estar `Ready`. Interrompa a sequência se o build falhar; a imagem `jacafi/server:k8s-local` precisa existir antes do deploy. Se o Compose estiver usando as portas 8081/8082, execute `docker compose stop server keycloak` para liberá-las sem remover volumes.

### 2. Definir as credenciais locais

Crie o arquivo local de credenciais uma única vez e substitua todos os valores `replace-with-...` no editor. O arquivo `k8s/secrets.env` é ignorado pelo Git; não versione credenciais nem salve o YAML gerado do Secret. Mantenha `RESEND_API_KEY` vazio se não usar e-mail.

```powershell
if (-not (Test-Path k8s/secrets.env)) {
    Copy-Item k8s/secrets.env.example k8s/secrets.env
}
notepad k8s/secrets.env
```

`DB_USERNAME` e `DB_PASSWORD` serão as credenciais do novo PostgreSQL; `KEYCLOAK_ADMIN` e `KEYCLOAK_ADMIN_PASSWORD` permitem entrar no console administrativo. Elas não substituem as contas de teste importadas pelo realm. Use valores próprios e não deixe os placeholders do exemplo.

### 3. Aplicar os recursos e aguardar a inicialização

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

O resultado esperado é um Pod `Running` e `1/1` para cada Deployment (`api`, `database`, `keycloak`), três Services e o PVC `database-data` em `Bound`. Se um rollout atingir o timeout, consulte os eventos e logs na seção de diagnóstico antes de prosseguir. Downloads de imagens e o primeiro startup podem exigir mais de cinco minutos em uma máquina com poucos recursos.

O Kustomize aplica Namespace, configuração pública, Deployments, Services e PVC; não cria o Secret nem o ConfigMap do realm e não habilita HPA. A API aguarda o banco e executa as migrações Flyway na inicialização. Os Services são internos (`ClusterIP`).

Se a API apresentar `ImagePullBackOff`/`ErrImagePull`, confira `kubectl -n jacafi describe pod <nome-do-pod>` e se o build usou o Docker Engine do Desktop. A disponibilidade de imagens locais varia com o provisionador (kubeadm ou kind) e o armazenamento de imagens do Desktop. Com kind, pode ser necessário carregar a imagem nos nós ou usar um registry acessível; verifique a configuração efetiva antes de assumir que a imagem construída está disponível. Caso seu cluster seja gerenciável pelo CLI `kind`, use `kind get clusters` para identificar o nome e `kind load docker-image jacafi/server:k8s-local --name <nome-do-cluster>`. Não presuma que o nome do contexto seja o nome do cluster kind.

## Acesso e smoke test

### 4. Abrir os acessos locais

Libere as portas 8081 e 8082 caso o Compose esteja em execução. Em dois terminais separados, mantenha os encaminhamentos ativos:

```powershell
kubectl -n jacafi port-forward service/api 8082:8082
```

```powershell
kubectl -n jacafi port-forward service/keycloak 8081:8080
```

Em um terceiro terminal, verifique a saúde e obtenha um token do usuário de desenvolvimento presente no realm:

### 5. Validar saúde e autenticação

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

O issuer público é `http://localhost:8081/realms/jacafi`; a API busca as chaves por `http://keycloak:8080/realms/jacafi/protocol/openid-connect/certs`. A diferença é intencional. Ao mudar a porta pública ou hostname do Keycloak, ajuste também `KC_HOSTNAME` em `overlays/local/keycloak.yaml` e `KEYCLOAK_ISSUER_URI` em `overlays/local/configmap.yaml`. Reinicie o port-forward se o Pod encaminhado for substituído.

### 6. Testar escrita e leitura no banco

No mesmo terceiro terminal, use o token anterior e dados exclusivos de demonstração:

```powershell
$employeeHeaders = @{ Authorization = "Bearer $($tokenResponse.access_token)" }
$customerBody = @{
    taxId = '52998224725'
    name = 'Cliente Teste Kubernetes'
    email = 'kubernetes@example.com'
    phone = '11999999999'
} | ConvertTo-Json
$customer = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/customers -Headers $employeeHeaders -ContentType 'application/json' -Body $customerBody
$customerId = $customer.id
Invoke-RestMethod -Uri "http://localhost:8082/api/v1/customers/$customerId" -Headers $employeeHeaders
```

O cadastro deve retornar um `id`, e a consulta deve retornar o mesmo cliente. Se esse CPF de exemplo já existir, não repita o cadastro: recupere-o com o comando abaixo e continue o teste com o ID existente.

```powershell
$customer = Invoke-RestMethod -Uri 'http://localhost:8082/api/v1/customers/lookup?taxId=52998224725' -Headers $employeeHeaders
$customerId = $customer.id
```

### 7. Confirmar bloqueios de acesso

Sem autenticação, a listagem deve responder `401`. Os comandos abaixo imprimem o status esperado e falham se a API permitir acesso ou devolver outro erro:

```powershell
try {
    Invoke-RestMethod http://localhost:8082/api/v1/customers
    throw 'A API permitiu acesso sem token.'
} catch {
    if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) { throw }
    '401 confirmado: token ausente.'
}
$customerTokenResponse = Invoke-RestMethod -Method Post -Uri http://localhost:8081/realms/jacafi/protocol/openid-connect/token -ContentType 'application/x-www-form-urlencoded' -Body @{
    client_id = 'jacafi-web'
    grant_type = 'password'
    username = 'dev-customer'
    password = 'Dev-customer-2026'
}
try {
    Invoke-RestMethod -Uri http://localhost:8082/api/v1/customers -Headers @{ Authorization = "Bearer $($customerTokenResponse.access_token)" }
    throw 'A API permitiu a listagem administrativa ao cliente.'
} catch {
    if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 403) { throw }
    '403 confirmado: role customer sem acesso administrativo.'
}
```

Para testar `/customers/me`, primeiro vincule a identidade do cliente conforme o [README principal](../README.md#autenticação). O login por si só não cria um cadastro de cliente na oficina.

### 8. Confirmar persistência após recriação do Pod

Em ambiente local de teste, reinicie apenas o Deployment do PostgreSQL. Isso causa uma interrupção breve de acesso ao banco, mas mantém o PVC:

```powershell
kubectl -n jacafi rollout restart deployment/database
kubectl -n jacafi rollout status deployment/database --timeout=300s
kubectl -n jacafi get pvc database-data
Invoke-RestMethod http://localhost:8082/actuator/health/readiness
Invoke-RestMethod -Uri "http://localhost:8082/api/v1/customers/$customerId" -Headers $employeeHeaders
```

Espere a readiness voltar a `UP` antes da consulta. O cliente deve manter o mesmo ID e dados. Se o token expirar, repita o login do funcionário e atualize `$employeeHeaders`. Se o port-forward encerrar, abra-o novamente. Esse teste comprova retenção no PVC durante substituição do Pod; não comprova recuperação de desastre.

### 9. Conferir pelo Swagger

Abra `http://localhost:8082/swagger-ui/index.html`. No terminal, consulte `$tokenResponse.access_token`, copie o token do funcionário e cole no campo bearer do botão **Authorize**. Execute a listagem e a consulta por ID dos testes anteriores. Não use a antiga conta `dev-admin`: a autenticação atual usa Keycloak.

| Verificação | Resultado esperado |
|---|---|
| Deployments e PVC | Três Pods prontos e PVC `Bound` |
| Liveness e readiness | `UP` |
| Login de funcionário | Token emitido pelo realm `jacafi` |
| Listagem com funcionário | HTTP 200 |
| Cadastro e consulta | Mesmo ID e dados |
| Listagem sem token | HTTP 401 |
| Listagem com cliente | HTTP 403 |
| Reinício do PostgreSQL | Dados do cliente preservados |

O HPA é uma verificação separada: os testes acima não comprovam escalonamento. Siga os pré-requisitos da seção seguinte antes de habilitá-lo.

## Atualizações e diagnóstico

Para mudar configuração pública compartilhada, edite `k8s/base/configmap.yaml`; valores específicos do ambiente ficam no `configmap.yaml` do respectivo overlay. Reaplique o overlay e reinicie os consumidores afetados: variáveis de ambiente não são atualizadas em Pods existentes.

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

## HPA e bloqueio de concorrência da outbox

O componente `components/autoscaling` é incluído somente por `load-test` e `eks`. Define CPU média alvo de 70% dos requests e memória média de 800 MiB por Pod, mínimo de 1 e máximo de 3 réplicas. Remove `spec.replicas` para deixar a escala com o HPA e configura `RollingUpdate` com uma réplica adicional durante atualização. Não aplique o YAML do componente isoladamente.

**O uso operacional de EKS com escritas depende de corrigir a outbox:** cada réplica executa os schedulers, e os processadores não fazem claim/locking dos eventos entre consumidores. HPA e rolling update podem provocar processamento duplicado mesmo com mínimo de uma réplica. Corrija e teste coordenação/idempotência antes da liberação. O local mantém uma réplica e `Recreate`; o teste de carga abaixo é exclusivamente de leitura, com banco isolado sem eventos pendentes e sem escritores concorrentes.

O HPA requer Metrics Server funcional e requests de CPU/memória (já definidos na API). No EKS, o Terraform instala o add-on `metrics-server` ([infra/eks_addon.tf](../infra/eks_addon.tf)); estes manifests não instalam addons. Verifique compatibilidade, aggregation layer, autenticação/autorização do kubelet, certificados e conectividade conforme a [documentação do Metrics Server](https://kubernetes-sigs.github.io/metrics-server/). Confira as métricas:

```powershell
kubectl get apiservice v1beta1.metrics.k8s.io
kubectl top nodes
kubectl -n jacafi-load-test top pods
```

Se o addon ainda não estiver instalado, a instalação local abaixo usa o manifesto oficial da versão 0.9.0. Execute somente no Docker Desktop, após conferir o contexto; no EKS, use o add-on instalado pelo Terraform.

```powershell
if ((kubectl config current-context) -ne 'docker-desktop') { throw 'Selecione o contexto docker-desktop antes de instalar o addon local.' }
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.9.0/components.yaml
kubectl -n kube-system rollout status deployment/metrics-server --timeout=180s
kubectl -n kube-system logs deployment/metrics-server --tail=50
```

Se o rollout não concluir e os logs mostrarem erro `x509` por certificado do kubelet sem IP SAN, a solução permanente é corrigir os certificados da plataforma. Para esse laboratório local, o comando abaixo acrescenta `--kubelet-insecure-tls` e desabilita a validação do certificado do kubelet. Execute uma única vez, somente para esse erro, e **não transporte essa opção para EKS**:

```powershell
if ((kubectl config current-context) -ne 'docker-desktop') { throw 'Esta exceção TLS é exclusiva do laboratório docker-desktop.' }
kubectl -n kube-system patch deployment metrics-server --type=json --patch '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
kubectl -n kube-system rollout status deployment/metrics-server --timeout=180s
kubectl top nodes
kubectl -n jacafi-load-test top pods
```

A coleta inicial pode levar alguns instantes após o addon ficar pronto. Não inicie a carga enquanto `kubectl top` não apresentar métricas.

Depois de aplicar um overlay com HPA, acompanhe no namespace correspondente:

```powershell
kubectl -n jacafi-load-test get hpa api --watch
kubectl -n jacafi-load-test describe hpa api
```

Consulte o [funcionamento do Horizontal Pod Autoscaler](https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/). O controlador usa a maior recomendação entre CPU e memória. O alvo de memória de 800 MiB é independente do request de 512 MiB e fica abaixo do limite de 1 GiB. Foi ajustado após observar que 80% do request mantinha três JVMs ociosas; calibre novamente com dados representativos na AWS. Métricas `<unknown>` durante inicialização podem ser transitórias; se persistirem, diagnostique o Metrics Server e as condições do HPA. A memória retida pela JVM pode impedir redução imediata após a carga; observe métricas e a janela padrão de estabilização de redução, normalmente cinco minutos. HPA cria Pods, mas não adiciona nós: é necessária capacidade para três réplicas, a réplica temporária de rollout e os demais serviços.

## Carga local isolada com NodePort

O overlay de carga reduz a janela de estabilização de scale-down para 60 segundos para facilitar a demonstração. O overlay EKS preserva o padrão do Kubernetes, normalmente 300 segundos. A redução também depende das métricas e do arredondamento do número de réplicas; não é imediata ao terminar o k6.

Use um namespace novo ou confirme que seu banco de teste não contém eventos pendentes. Não execute os testes de cadastro/escrita acima neste ambiente enquanto houver múltiplas réplicas. O banco `jacafi-load-test` é separado do banco do namespace `jacafi`; não altere sua URL para apontar ao banco de trabalho.

Com o contexto `docker-desktop`, imagem local construída e Metrics Server funcional, reutilize o arquivo local de credenciais preparado anteriormente:

```powershell
kubectl config use-context docker-desktop
if ($LASTEXITCODE -ne 0 -or (kubectl config current-context) -ne 'docker-desktop') { throw 'Contexto local não selecionado.' }
kubectl create namespace jacafi-load-test --dry-run=client -o yaml | kubectl apply -f -
kubectl -n jacafi-load-test create secret generic jacafi-secrets --from-env-file=k8s/secrets.env --dry-run=client -o yaml | kubectl apply -f -
kubectl -n jacafi-load-test create configmap keycloak-realm --from-file=jacafi-realm.json=keycloak/jacafi-realm.json --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -k k8s/overlays/load-test
kubectl -n jacafi-load-test rollout status deployment/database --timeout=300s
kubectl -n jacafi-load-test rollout status deployment/keycloak --timeout=300s
kubectl -n jacafi-load-test rollout status deployment/api --timeout=300s
Invoke-RestMethod http://localhost:30082/actuator/health/readiness
$loadToken = Invoke-RestMethod -Method Post -Uri http://localhost:30081/realms/jacafi/protocol/openid-connect/token -ContentType 'application/x-www-form-urlencoded' -Body @{
    client_id = 'jacafi-web'
    grant_type = 'password'
    username = 'dev-employee'
    password = 'Dev-employee-2026'
}
Invoke-RestMethod -Uri http://localhost:30082/api/v1/customers -Headers @{ Authorization = "Bearer $($loadToken.access_token)" }
kubectl -n jacafi-load-test get hpa,pods
```

O tráfego da carga deve entrar pelo NodePort, distribuído pelo Service entre réplicas. Um `port-forward` de Service fixa um Pod e não serve para medir distribuição. Se o provisionador do Desktop não expuser NodePorts em localhost, configure o acesso às portas dos nós conforme seu provisionador e ajuste o hostname público/issuer do Keycloak de forma consistente; não substitua silenciosamente o acesso por port-forward para esse teste. Renove tokens antes de expirar (15 minutos no realm local).

Use o script k6 de leitura versionado em [k8s/load-test.k6.js](load-test.k6.js) (que executa exclusivamente requisições `GET` autenticadas em `/api/v1/customers` e `/api/v1/service-orders`):

```powershell
k6 run k8s/load-test.k6.js
```

Para apontar o mesmo script ao ambiente AWS EKS, sobrescreva `API_URL` e `KEYCLOAK_URL`:

```powershell
k6 run -e API_URL="http://$ALB" -e KEYCLOAK_URL="http://$ALB:8080" k8s/load-test.k6.js
```

### Evidência local de escalonamento

Em 21/10/2026, no Docker Desktop, o teste de leitura autenticada executou 86.424 consultas de clientes, com 100% HTTP 200, zero falhas HTTP e p95 de 3,71 ms. A carga durou quatro minutos, chegando a 80 usuários virtuais. Os eventos `SuccessfulRescale` registraram aumento de 1 para 2 e depois 3 réplicas por CPU acima do alvo. A outbox estava vazia e o envio de e-mails desabilitado.

Após a carga, o alvo inicial de memória de 80% do request mantinha três JVMs ociosas. A configuração foi ajustada para média de 800 MiB e janela de redução de 60 segundos no overlay de carga; o HPA voltou a reduzir as réplicas automaticamente. Esses resultados são de um banco local sem clientes e não representam capacidade de produção, testes de escrita concorrente ou validação AWS.

## AWS EKS

O overlay `overlays/eks` é aplicado pelo pipeline ([Fluxo CI/CD](../.github/workflows/ci.yaml)) sobre a infraestrutura de [infra/](../infra/README.md): EKS, RDS PostgreSQL e um ALB, todos criados pelo Terraform. O overlay reaproveita a API de `base/`, o Keycloak de `components/keycloak` e o HPA de `components/autoscaling`. Os dois Services ficam como NodePort, que é por onde o ALB alcança os nós:

| Entrada pública | Service | NodePort |
|---|---|---|
| `http://<alb>` | `api` | 30080 |
| `http://<alb>:8080` | `keycloak` | 30081 |

Alguns valores só existem depois do `terraform apply`. Eles ficam no overlay como placeholders `${EKS_*}`. No deploy, o `envsubst` os preenche numa cópia dos arquivos do overlay, antes do `kustomize`. A ordem importa: assim os valores entre aspas continuam strings no YAML gerado.

| Placeholder | Origem |
|---|---|
| `EKS_IMAGE_REPOSITORY` | variável `ECR_REPOSITORY_URL` do repositório |
| `EKS_IMAGE_TAG` | SHA do commit publicado no ECR |
| `EKS_ALB_DNS` | output `alb_dns` do Terraform |
| `EKS_DB_HOST` | output `db_host` do Terraform |
| `EKS_RESEND_FROM` | variável `RESEND_FROM` do repositório |
| `EKS_RESEND_ENABLED` | `true` quando existem o secret `RESEND_API_KEY` e a variável `RESEND_FROM` |

Antes de aplicar o overlay, o pipeline cria três objetos:

- o Secret `jacafi-secrets`, a partir dos secrets do repositório;
- o ConfigMap `keycloak-realm`, com o [realm versionado](../keycloak/jacafi-realm.json);
- o ConfigMap `rds-ca`, com o bundle oficial de certificados da AWS. A conexão com o RDS usa `sslmode=verify-full`.

Diferenças em relação ao ambiente local:

- **Banco:** fica no RDS; não há Deployment `database` nem PVC no cluster.
- **Keycloak:** roda em `start-dev` e reimporta o realm a cada subida. O issuer é `http://<alb>:8080/realms/jacafi`, e a API busca as chaves pelo Service interno `keycloak`.
- **Swagger e OpenAPI:** habilitados no overlay efêmero do EKS (`http://<alb>/swagger-ui/index.html`) para facilitar a exploração manual e a demonstração em vídeo.
- **Metrics Server:** é instalado pelo Terraform como add-on do EKS.

A observação sobre a outbox vale aqui também: para demonstrar o HPA, use carga de leitura.

Para reproduzir o deploy manualmente, use bash (Linux, macOS, WSL ou Git Bash), com o ambiente já provisionado e credenciais AWS de um principal com acesso ao cluster. O arquivo de credenciais precisa conter `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_ADMIN` e `KEYCLOAK_ADMIN_PASSWORD`; `RESEND_API_KEY` é opcional.

```bash
export EKS_ALB_DNS=$(terraform -chdir=infra output -raw alb_dns)
export EKS_DB_HOST=$(terraform -chdir=infra output -raw db_host)
export EKS_IMAGE_REPOSITORY=<ecr_repository_url> EKS_IMAGE_TAG=<sha-publicado>
export EKS_RESEND_ENABLED=false EKS_RESEND_FROM=
aws eks update-kubeconfig --name jacafi-eks --region us-east-2
kubectl apply -f k8s/base/namespace.yaml
kubectl -n jacafi create secret generic jacafi-secrets --from-env-file=<arquivo-seguro> --dry-run=client -o yaml | kubectl apply -f -
kubectl -n jacafi create configmap keycloak-realm --from-file=jacafi-realm.json=keycloak/jacafi-realm.json --dry-run=client -o yaml | kubectl apply -f -
curl -fsSL https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -o global-bundle.pem
kubectl -n jacafi create configmap rds-ca --from-file=global-bundle.pem=global-bundle.pem --dry-run=client -o yaml | kubectl apply -f -
rm -rf /tmp/k8s-eks && cp -r k8s /tmp/k8s-eks
for file in /tmp/k8s-eks/overlays/eks/*.yaml; do
  envsubst '${EKS_IMAGE_REPOSITORY} ${EKS_IMAGE_TAG} ${EKS_ALB_DNS} ${EKS_DB_HOST} ${EKS_RESEND_ENABLED} ${EKS_RESEND_FROM}' < "$file" > "$file.tmp" && mv "$file.tmp" "$file"
done
kubectl apply -k /tmp/k8s-eks/overlays/eks
kubectl -n jacafi rollout status deployment/keycloak --timeout=600s
kubectl -n jacafi rollout status deployment/api --timeout=600s
```

## Encerramento e dados

Para parar os workloads mantendo PVC, Secret e configurações, encerre os port-forwards com Ctrl+C, remova o HPA se tiver sido habilitado e reduza os Deployments a zero:

```powershell
kubectl -n jacafi delete hpa api --ignore-not-found
kubectl -n jacafi scale deployment/api deployment/keycloak deployment/database --replicas=0
```

Para retomar, execute `kubectl apply -k k8s` e repita os waits e port-forwards. O PostgreSQL reutiliza os dados; o Keycloak reimporta o realm.

Para pausar a carga, use os mesmos comandos com `-n jacafi-load-test`. Para retomar, reaplique o overlay e restaure explicitamente uma réplica da API: o overlay omite `spec.replicas`, e o HPA fica inativo quando a escala desejada está em zero.

```powershell
kubectl apply -k k8s/overlays/load-test
kubectl -n jacafi-load-test scale deployment/api --replicas=1
kubectl -n jacafi-load-test rollout status deployment/database --timeout=300s
kubectl -n jacafi-load-test rollout status deployment/keycloak --timeout=300s
kubectl -n jacafi-load-test rollout status deployment/api --timeout=300s
```

Reaplicar esse overlay recria também o HPA. As instruções de limpeza local não desprovisionam a AWS: para remover EKS, RDS e ALB, use o workflow **Derrubar ambiente AWS** descrito em [infra/README.md](../infra/README.md).

**A exclusão do namespace remove também o PVC e pode apagar definitivamente os dados do volume**, conforme a política de reclaim do provisionador. Faça backup antes. Execute apenas se deseja descartar todo o ambiente:

```powershell
kubectl delete namespace jacafi
```

`kubectl delete -k k8s` também inclui Namespace e PVC: não use esse comando para uma pausa que deve preservar dados.

## Validação dos manifests

Para inspecionar o resultado sem executar workloads:

```powershell
kubectl kustomize k8s
kubectl kustomize k8s/overlays/load-test
kubectl kustomize k8s/overlays/eks
kubectl apply --dry-run=server -k k8s
```

O dry-run dos recursos namespaced requer que `jacafi` já exista: o Namespace simulado não persiste durante esse comando. Renderização e validação pelo API server não comprovam subida dos containers, persistência, smoke test nem comportamento do HPA; execute as verificações deste guia no ambiente de destino.
