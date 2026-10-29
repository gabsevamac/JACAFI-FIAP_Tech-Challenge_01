output "alb_dns" {
  description = "Endereço do Application Load Balancer"
  value       = aws_lb.main.dns_name
}

output "api_url" {
  description = "URL pública da API"
  value       = "http://${aws_lb.main.dns_name}"
}

output "keycloak_url" {
  description = "URL pública do Keycloak (issuer: <keycloak_url>/realms/jacafi)"
  value       = "http://${aws_lb.main.dns_name}:${var.keycloak_listener_port}"
}

output "cluster_name" {
  description = "Nome do cluster EKS"
  value       = aws_eks_cluster.main.name
}

output "aws_region" {
  description = "Região da AWS usada no projeto"
  value       = var.aws_region
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

output "db_username" {
  value = aws_db_instance.main.username
}
