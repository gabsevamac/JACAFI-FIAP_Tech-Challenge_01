# O plano Free da nova experiência da AWS bloqueia provedores de identidade (iam:*Provider*), então o
# pipeline não usa OIDC: ele se autentica com a access key de um usuário IAM sem acesso ao console.
#
# As permissões seguem o menor privilégio. Cada ação listada aqui é usada pelos workflows ou pelo stack
# infra/, e os recursos ficam restritos aos nomes do projeto sempre que o serviço permite. A access key
# não é criada pelo Terraform, para o segredo não ir parar no estado (veja infra/README.md).

locals {
  conta = data.aws_caller_identity.current.account_id

  # Precisam bater com infra/ e com os workflows (EKS_CLUSTER_NAME, infra/backend.tf).
  cluster_name    = "${var.projeto}-eks"
  node_group_name = "${var.projeto}-nodes"
  tfstate_key     = "jacafi/infra.tfstate"

  eks_cluster_role_arn = "arn:aws:iam::${local.conta}:role/${var.projeto}-eks-cluster"
  eks_nodes_role_arn   = "arn:aws:iam::${local.conta}:role/${var.projeto}-eks-nodes"

  eks_arn = "arn:aws:eks:${var.aws_region}:${local.conta}"
  elb_arn = "arn:aws:elasticloadbalancing:${var.aws_region}:${local.conta}"
  rds_arn = "arn:aws:rds:${var.aws_region}:${local.conta}"
}

resource "aws_iam_user" "pipeline" {
  name = "${var.projeto}-github-actions"
}

#===============
# Estado do Terraform, ECR e acesso ao cluster
#===============
data "aws_iam_policy_document" "pipeline_artefatos" {
  statement {
    sid       = "EstadoListar"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.tfstate.arn]
  }

  statement {
    sid       = "EstadoLerGravar"
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["${aws_s3_bucket.tfstate.arn}/${local.tfstate_key}"]
  }

  # use_lockfile = true cria e apaga o .tflock a cada operação.
  statement {
    sid       = "EstadoLock"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.tfstate.arn}/${local.tfstate_key}.tflock"]
  }

  # GetAuthorizationToken não aceita restrição por recurso.
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [aws_ecr_repository.api.arn]
  }
}

#===============
# Rede, load balancer e vínculo dos nós com os target groups
#===============
data "aws_iam_policy_document" "pipeline_rede" {
  # As ações Describe da EC2, do ELB e do Auto Scaling não aceitam restrição por recurso.
  # O ELB consulta ec2:GetSecurityGroupsForVpc com a credencial de quem cria o load balancer.
  statement {
    sid = "Leitura"
    actions = [
      "autoscaling:Describe*",
      "ec2:Describe*",
      "ec2:GetSecurityGroupsForVpc",
      "elasticloadbalancing:Describe*",
    ]
    resources = ["*"]
  }

  # A EC2 gera IDs aleatórios para VPC, subnets e security groups, então a restrição é por ação e região.
  # Não há ec2:RunInstances: as instâncias dos nós são criadas pelo próprio EKS.
  statement {
    sid = "RedeDoAmbiente"
    actions = [
      "ec2:CreateVpc",
      "ec2:DeleteVpc",
      "ec2:ModifyVpcAttribute",
      "ec2:CreateSubnet",
      "ec2:DeleteSubnet",
      "ec2:ModifySubnetAttribute",
      "ec2:CreateInternetGateway",
      "ec2:DeleteInternetGateway",
      "ec2:AttachInternetGateway",
      "ec2:DetachInternetGateway",
      "ec2:CreateRouteTable",
      "ec2:DeleteRouteTable",
      "ec2:CreateRoute",
      "ec2:ReplaceRoute",
      "ec2:DeleteRoute",
      "ec2:AssociateRouteTable",
      "ec2:DisassociateRouteTable",
      "ec2:ReplaceRouteTableAssociation",
      "ec2:CreateSecurityGroup",
      "ec2:DeleteSecurityGroup",
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:RevokeSecurityGroupIngress",
      "ec2:RevokeSecurityGroupEgress",
      "ec2:ModifySecurityGroupRules",
      "ec2:UpdateSecurityGroupRuleDescriptionsIngress",
      "ec2:UpdateSecurityGroupRuleDescriptionsEgress",
      "ec2:CreateTags",
      "ec2:DeleteTags",
    ]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestedRegion"
      values   = [var.aws_region]
    }
  }

  statement {
    sid = "LoadBalancerDoAmbiente"
    actions = [
      "elasticloadbalancing:CreateLoadBalancer",
      "elasticloadbalancing:DeleteLoadBalancer",
      "elasticloadbalancing:ModifyLoadBalancerAttributes",
      "elasticloadbalancing:SetSecurityGroups",
      "elasticloadbalancing:SetSubnets",
      "elasticloadbalancing:SetIpAddressType",
      "elasticloadbalancing:CreateTargetGroup",
      "elasticloadbalancing:DeleteTargetGroup",
      "elasticloadbalancing:ModifyTargetGroup",
      "elasticloadbalancing:ModifyTargetGroupAttributes",
      "elasticloadbalancing:CreateListener",
      "elasticloadbalancing:DeleteListener",
      "elasticloadbalancing:ModifyListener",
      "elasticloadbalancing:ModifyListenerAttributes",
      "elasticloadbalancing:AddTags",
      "elasticloadbalancing:RemoveTags",
    ]
    resources = [
      "${local.elb_arn}:loadbalancer/app/${var.projeto}-alb/*",
      "${local.elb_arn}:listener/app/${var.projeto}-alb/*",
      "${local.elb_arn}:targetgroup/${var.projeto}-*/*",
    ]
  }

  # O EKS nomeia o Auto Scaling group do node group como eks-<node group>-<id>.
  statement {
    sid = "NosNosTargetGroups"
    actions = [
      "autoscaling:AttachLoadBalancerTargetGroups",
      "autoscaling:DetachLoadBalancerTargetGroups",
      "autoscaling:AttachTrafficSources",
      "autoscaling:DetachTrafficSources",
    ]
    resources = ["arn:aws:autoscaling:${var.aws_region}:${local.conta}:autoScalingGroup:*:autoScalingGroupName/eks-${local.node_group_name}-*"]
  }
}

#===============
# EKS, RDS e as roles IAM do cluster
#===============
data "aws_iam_policy_document" "pipeline_plataforma" {
  # Inclui o eks:DescribeCluster usado pelo deploy e pela decisão de atualizar o ambiente.
  statement {
    sid = "Leitura"
    actions = [
      "eks:Describe*",
      "eks:List*",
      "rds:Describe*",
      "rds:ListTagsForResource",
    ]
    resources = ["*"]
  }

  # eks:CreateCluster não aceita restrição por recurso; a condição exige a tag do projeto (default_tags).
  statement {
    sid       = "EksCriarCluster"
    actions   = ["eks:CreateCluster"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "aws:RequestTag/Projeto"
      values   = [replace(var.projeto, "-", "_")]
    }
  }

  statement {
    sid = "EksDoAmbiente"
    actions = [
      "eks:DeleteCluster",
      "eks:UpdateClusterConfig",
      "eks:UpdateClusterVersion",
      "eks:CreateNodegroup",
      "eks:DeleteNodegroup",
      "eks:UpdateNodegroupConfig",
      "eks:UpdateNodegroupVersion",
      "eks:CreateAddon",
      "eks:DeleteAddon",
      "eks:UpdateAddon",
      "eks:CreateAccessEntry",
      "eks:DeleteAccessEntry",
      "eks:UpdateAccessEntry",
      "eks:AssociateAccessPolicy",
      "eks:DisassociateAccessPolicy",
      "eks:TagResource",
      "eks:UntagResource",
    ]
    resources = [
      "${local.eks_arn}:cluster/${local.cluster_name}",
      "${local.eks_arn}:nodegroup/${local.cluster_name}/${local.node_group_name}/*",
      "${local.eks_arn}:addon/${local.cluster_name}/*",
      "${local.eks_arn}:access-entry/${local.cluster_name}/*",
    ]
  }

  # Só PostgreSQL na classe definida: a chave não consegue criar um banco maior nem de outro motor.
  statement {
    sid       = "RdsCriarBanco"
    actions   = ["rds:CreateDBInstance"]
    resources = ["${local.rds_arn}:db:${var.projeto}-postgres"]

    condition {
      test     = "StringEquals"
      variable = "rds:DatabaseEngine"
      values   = ["postgres"]
    }

    condition {
      test     = "StringEquals"
      variable = "rds:DatabaseClass"
      values   = [var.db_instance_class]
    }
  }

  # A criação do banco também é autorizada contra o subnet group e os grupos de opções e parâmetros padrão.
  statement {
    sid     = "RdsGruposDoBanco"
    actions = ["rds:CreateDBInstance"]
    resources = [
      "${local.rds_arn}:subgrp:${var.projeto}-db-subnets",
      "${local.rds_arn}:og:*",
      "${local.rds_arn}:pg:*",
    ]
  }

  statement {
    sid = "RdsDoAmbiente"
    actions = [
      "rds:ModifyDBInstance",
      "rds:DeleteDBInstance",
      "rds:CreateDBSubnetGroup",
      "rds:ModifyDBSubnetGroup",
      "rds:DeleteDBSubnetGroup",
      "rds:AddTagsToResource",
      "rds:RemoveTagsFromResource",
    ]
    resources = [
      "${local.rds_arn}:db:${var.projeto}-postgres",
      "${local.rds_arn}:subgrp:${var.projeto}-db-subnets",
      "${local.rds_arn}:og:*",
      "${local.rds_arn}:pg:*",
    ]
  }

  # Sem iam:PutRolePolicy: as roles só recebem as policies gerenciadas do EKS listadas abaixo.
  statement {
    sid = "RolesDoCluster"
    actions = [
      "iam:CreateRole",
      "iam:DeleteRole",
      "iam:GetRole",
      "iam:TagRole",
      "iam:UntagRole",
      "iam:UpdateAssumeRolePolicy",
      "iam:ListRolePolicies",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
    ]
    resources = [local.eks_cluster_role_arn, local.eks_nodes_role_arn]
  }

  statement {
    sid       = "PoliciesDaRoleDoCluster"
    actions   = ["iam:AttachRolePolicy", "iam:DetachRolePolicy"]
    resources = [local.eks_cluster_role_arn]

    condition {
      test     = "ArnEquals"
      variable = "iam:PolicyARN"
      values   = ["arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"]
    }
  }

  statement {
    sid       = "PoliciesDaRoleDosNos"
    actions   = ["iam:AttachRolePolicy", "iam:DetachRolePolicy"]
    resources = [local.eks_nodes_role_arn]

    condition {
      test     = "ArnEquals"
      variable = "iam:PolicyARN"
      values = [
        "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
        "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
        "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
      ]
    }
  }

  statement {
    sid       = "PassarRolesAoEks"
    actions   = ["iam:PassRole"]
    resources = [local.eks_cluster_role_arn, local.eks_nodes_role_arn]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["eks.amazonaws.com"]
    }
  }

  # Caso o EKS deixe para trás o instance profile dos nós, o destroy da role consegue removê-lo.
  statement {
    sid       = "InstanceProfileDosNos"
    actions   = ["iam:RemoveRoleFromInstanceProfile"]
    resources = ["arn:aws:iam::${local.conta}:instance-profile/eks-*"]
  }

  # Numa conta nova, a primeira criação de cada recurso também cria a role vinculada ao serviço.
  statement {
    sid       = "RolesVinculadasAosServicos"
    actions   = ["iam:CreateServiceLinkedRole"]
    resources = ["arn:aws:iam::${local.conta}:role/aws-service-role/*"]

    condition {
      test     = "StringEquals"
      variable = "iam:AWSServiceName"
      values = [
        "autoscaling.amazonaws.com",
        "eks.amazonaws.com",
        "eks-nodegroup.amazonaws.com",
        "elasticloadbalancing.amazonaws.com",
        "rds.amazonaws.com",
      ]
    }
  }

  # O EKS confere, com a credencial de quem chama, se a role vinculada ao serviço já existe. A consulta
  # usa só o nome da role, então os dois formatos de ARN (com e sem o caminho aws-service-role/) são liberados.
  statement {
    sid     = "LerRolesVinculadasAosServicos"
    actions = ["iam:GetRole"]
    resources = [
      "arn:aws:iam::${local.conta}:role/AWSServiceRoleFor*",
      "arn:aws:iam::${local.conta}:role/aws-service-role/*",
    ]
  }
}

#===============
# Policies gerenciadas (o limite de policies inline de um usuário é de 2.048 caracteres)
#===============
locals {
  pipeline_policies = {
    artefatos  = data.aws_iam_policy_document.pipeline_artefatos.json
    rede       = data.aws_iam_policy_document.pipeline_rede.json
    plataforma = data.aws_iam_policy_document.pipeline_plataforma.json
  }
}

resource "aws_iam_policy" "pipeline" {
  for_each = local.pipeline_policies

  name   = "${var.projeto}-pipeline-${each.key}"
  policy = each.value
}

resource "aws_iam_user_policy_attachment" "pipeline" {
  for_each = aws_iam_policy.pipeline

  user       = aws_iam_user.pipeline.name
  policy_arn = each.value.arn
}
