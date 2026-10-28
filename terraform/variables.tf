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
  default     = "mecanica-fiap"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "cluster_name" {
  type    = string
  default = "mecanica-fiap-eks"
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.medium"]
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 2
}

variable "node_disk_size" {
  type    = number
  default = 20
}

variable "node_port" {
  description = "NodePort do Service da API (target group e security group)"
  type        = number
  default     = 30080
}

variable "health_check_path" {
  type    = string
  default = "/health"
}

variable "db_name" {
  type    = string
  default = "jacafi"
}

variable "db_username" {
  type    = string
  default = "postgres"
}

variable "db_engine_version" {
  type    = string
  default = "16"
}

variable "db_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}

variable "ecr_repository_name" {
  type    = string
  default = "mecanica-api-ecr"
}