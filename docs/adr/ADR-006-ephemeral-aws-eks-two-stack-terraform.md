# ADR-006 — Ambiente AWS Efêmero e Infraestrutura Terraform em Dois Stacks

## Status

Aceito.

## Contexto

A Fase 2 do projeto exige o provisionamento de um cluster Kubernetes gerenciado (AWS EKS), um banco de dados gerenciado (AWS RDS PostgreSQL) e uma esteira de CI/CD capaz de construir a imagem, atualizar o banco e publicar a aplicação no cluster.

Ao mesmo tempo, a conta AWS de avaliação opera sob créditos limitados (US$ 100) e sob restrições do plano Free da nova experiência da AWS:

1. **Custo contínuo do EKS:** apenas o plano de controle do EKS custa US$ 0,10/h, independentemente de haver pods em execução. Somado a 3 nós `t3.small`, um RDS `db.t4g.micro`, um Application Load Balancer e os IPs públicos IPv4, o ambiente ligado continuamente consome cerca de US$ 0,24/h (~US$ 5,80/dia), esgotando os créditos em aproximadamente três semanas.
2. **Bloqueio de OIDC no plano Free:** contas criadas no plano Free possuem uma *Service Control Policy* (SCP) da própria AWS que bloqueia ações `iam:*OpenIDConnectProvider*`, impedindo a federação OIDC direta com o GitHub Actions.
3. **Acoplamento com controladores externos:** instalar o *AWS Load Balancer Controller* via Helm + IRSA adiciona CRDs, papéis IAM extras e o risco de o Kubernetes criar recursos de rede por fora do estado do Terraform, que travam a exclusão da VPC durante um `terraform destroy`.

## Alternativas consideradas

- **Manter um único stack Terraform continuamente ligado:** simples de operar, mas inviável financeiramente dentro do limite de créditos e arriscado caso o ambiente seja esquecido ativo.
- **Destruir absolutamente todos os recursos a cada sessão (incluindo ECR e bucket S3 de estado):** exigiria recriar o bucket de estado, reenviar todas as imagens e reconfigurar segredos a cada execução.
- **Expor a aplicação via `Service` do tipo `LoadBalancer` ou `Ingress` com AWS Load Balancer Controller:** aumenta a complexidade de bootstrap (IRSA, Helm) e frequentemente deixa Load Balancers ou *Security Groups* órfãos que bloqueiam o `terraform destroy` automatizado.
- **Usar sub-redes privadas com NAT Gateway:** adiciona cerca de US$ 0,045/h mais custo por GB processado, sem ganho proporcional para um ambiente efêmero de demonstração acadêmica onde o RDS já permanece sem IP público (`publicly_accessible = false`) e isolado por *Security Group*.

## Decisão

### 1. Separação do Terraform em dois stacks (`infra/bootstrap` e `infra`)

A infraestrutura é dividida conforme o ciclo de vida:

- **`infra/bootstrap` (persistente):** aplicado uma única vez pelo administrador da conta. Cria o bucket S3 de estado (`jacafi-tfstate-<conta>`) com versionamento e lock nativo (`use_lockfile`), o repositório ECR (`jacafi/server`), o orçamento mensal (`AWS Budget`) e o usuário IAM da esteira (`jacafi-github-actions`). Custo próximo de zero quando o ambiente está desligado.
- **`infra` (efêmero):** aplicado e destruído pela esteira de CI/CD. Provisiona a VPC (`10.0.0.0/16` com 3 sub-redes públicas), o cluster EKS 1.36 (`jacafi-eks`) com 3 nós `t3.small` e add-on `metrics-server`, a instância RDS PostgreSQL 16 (`db.t4g.micro`, privada, sem retenção de snapshot final) e o Application Load Balancer.

### 2. Ciclo de vida efêmero controlado por workflows

- O ambiente sobe sob demanda via `workflow_dispatch` no workflow [ci.yaml](../../.github/workflows/ci.yaml). Em eventos de `push` na `main`, o job `aws_environment` consulta o status do cluster: se estiver `ACTIVE`, atualiza a infraestrutura e o deploy; se não existir, apenas publica a nova imagem no ECR sem incorrer em custos de EKS/RDS.
- O workflow [infra-destroy.yaml](../../.github/workflows/infra-destroy.yaml) destrói o stack `infra` manualmente (mediante confirmação `DESTRUIR`) ou automaticamente todos os dias às 03:00 UTC (meia-noite de Brasília), funcionando como rede de segurança contra esquecimento.

### 3. ALB gerenciado pelo Terraform apontando para NodePorts fixos

Em vez de depender do *AWS Load Balancer Controller*, o próprio Terraform cria o ALB, os *Target Groups* (`30080` para a API e `30081` para o Keycloak, com *health checks* em `/actuator/health/readiness` e `/realms/jacafi`) e os anexa diretamente ao *Auto Scaling Group* do *Node Group* do EKS. Os manifests em `k8s/overlays/eks` expõem os `Services` como `NodePort` nas mesmas portas. Assim, 100% dos recursos de rede pertencem ao estado do Terraform e são removidos de forma limpa e determinística no `terraform destroy`.

### 4. Autenticação do pipeline com usuário IAM de menor privilégio

Como o plano Free impede a criação do provedor OIDC, o bootstrap cria o usuário IAM `jacafi-github-actions` sem acesso ao console e com três políticas restritas (`jacafi-pipeline-artefatos`, `jacafi-pipeline-rede` e `jacafi-pipeline-plataforma`), limitando ações aos prefixos e tipos de instância do projeto (por exemplo, impedindo `ec2:RunInstances` diretos ou criação de bancos fora de `db.t4g.micro`). A *access key* é gerada fora do estado do Terraform e armazenada nos *Secrets* do GitHub.

## Consequências

- **Previsibilidade de custos:** 30 horas de uso em sessões de trabalho e gravação custam cerca de US$ 7, preservando a folga dos créditos da conta, enquanto o agendamento diário de destruição evita cobranças acidentais.
- **Destruição e recriação limpas:** o `terraform destroy` remove o cluster, o banco, o ALB e a VPC sem deixar recursos órfãos, preservando no `bootstrap` o histórico de imagens no ECR e o estado remoto no S3.
- **Compromissos assumidos para o ambiente de demonstração:** os dados do RDS não persistem entre sessões de destruição e recriação; a primeira subida completa leva cerca de 20 a 25 minutos; e o tráfego externo chega ao ALB em HTTP (portas `80` e `8080`), enquanto a conexão entre os pods da API e o RDS ocorre de forma privada com TLS `verify-full` validado pelo bundle global de certificados da AWS.

