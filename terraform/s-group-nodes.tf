resource "aws_vpc_security_group_ingress_rule" "nodes_from_alb" {
  security_group_id            = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = var.node_port
  to_port                      = var.node_port
  description                  = "NodePort da API vindo do ALB"
}

resource "aws_vpc_security_group_ingress_rule" "nodes_from_rds" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  description                  = "PostgreSQL vindo dos nos do EKS"
}