variable "db_password" {
  description = "Senha do usuário master do PostgreSQL"
  type        = string
  sensitive   = true
}

variable "aws_region" {
  description = "Região da AWS"
  type        = string
  default     = "us-east-1"
}

variable "projeto" {
  description = "Prefixo usado nos nomes dos recursos"
  type        = string
  default     = "jacafi"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "cluster_name" {
  type    = string
  default = "jacafi-eks"
}

variable "kubernetes_version" {
  description = "Versão do Kubernetes no EKS; mantenha uma versão em suporte padrão (o suporte estendido custa 6x mais)"
  type        = string
  default     = "1.36"
}

variable "cluster_admin_principal_arns" {
  description = "ARNs IAM, além da role do pipeline, com acesso de administrador ao cluster via kubectl"
  type        = list(string)
  default     = []
}

variable "node_instance_types" {
  description = "Tipos de instância dos nós; t3.small é aceito tanto no plano Free quanto no Paid da AWS"
  type        = list(string)
  default     = ["t3.small"]
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "node_disk_size" {
  type    = number
  default = 20
}

variable "api_node_port" {
  description = "NodePort do Service da API (target group e security group)"
  type        = number
  default     = 30080
}

variable "keycloak_node_port" {
  description = "NodePort do Service do Keycloak (target group e security group)"
  type        = number
  default     = 30081
}

variable "keycloak_listener_port" {
  description = "Porta pública do ALB que encaminha para o Keycloak"
  type        = number
  default     = 8080
}

variable "api_health_check_path" {
  type    = string
  default = "/actuator/health/readiness"
}

variable "keycloak_health_check_path" {
  type    = string
  default = "/realms/jacafi"
}

variable "db_name" {
  type    = string
  default = "jacafi"
}

variable "db_username" {
  type    = string
  default = "jacafi"
}

variable "db_engine_version" {
  type    = string
  default = "16"
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}
