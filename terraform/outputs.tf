output "alb_dns" {
  description = "Endereço do Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "cluster_name" {
  description = "Nome do cluster EKS"
  value       = aws_eks_cluster.main.name
}

output "ecr_repository_url" {
  description = "URL do repositório ECR para a pipeline"
  value       = aws_ecr_repository.api.repository_url
}

output "aws_region" {
  description = "Região da AWS usada no projeto"
  value       = "us-east-1"
}

output "db_host" {
  description = "Endereço do banco PostgreSQL"
  value       = aws_db_instance.main.address
}

output "db_port" {
  value = aws_db_instance.main.port
}

output "db_name" {
  value = aws_db_instance.main.db_name
}