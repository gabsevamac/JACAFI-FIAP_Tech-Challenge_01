# O EKS não instala o Metrics Server por padrão, e o HPA depende dele.
resource "aws_eks_addon" "metrics_server" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "metrics-server"

  depends_on = [aws_eks_node_group.main]
}
