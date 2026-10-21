resource "aws_eks_cluster" "main" {
  name     = var.cluster_name
  role_arn = data.aws_iam_role.lab_role.arn

  vpc_config {
    subnet_ids              = aws_subnet.sb_public[*].id
    endpoint_public_access  = true
    endpoint_private_access = false
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = false
  }

  depends_on = [aws_route_table_association.rta_public]
}