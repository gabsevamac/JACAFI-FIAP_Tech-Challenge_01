resource "aws_eks_cluster" "main" {
  name     = var.cluster_name
  version  = var.kubernetes_version
  role_arn = aws_iam_role.eks_cluster.arn

  vpc_config {
    subnet_ids              = aws_subnet.sb_public[*].id
    endpoint_public_access  = true
    endpoint_private_access = false
  }

  # Quem cria o cluster (o usuário IAM do pipeline) recebe acesso de administrador.
  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = true
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster,
    aws_route_table_association.rta_public,
  ]
}
