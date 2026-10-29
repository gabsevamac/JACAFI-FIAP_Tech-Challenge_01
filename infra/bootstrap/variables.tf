variable "aws_region" {
  description = "Região da AWS"
  type        = string
  default     = "us-east-1"
}

variable "projeto" {
  description = "Prefixo usado nos nomes dos recursos; precisa ser o mesmo de infra/"
  type        = string
  default     = "jacafi"
}

variable "github_repository" {
  description = "Repositório autorizado a assumir a role do pipeline, no formato dono/nome"
  type        = string
  default     = "gabsevamac/JACAFI-FIAP_Tech-Challenge_01"
}

variable "github_branch" {
  description = "Branch autorizada a assumir a role do pipeline"
  type        = string
  default     = "main"
}

variable "ecr_repository_name" {
  type    = string
  default = "jacafi/server"
}

variable "budget_limit_usd" {
  description = "Limite mensal do alerta de orçamento, em dólares, sem descontar créditos"
  type        = number
  default     = 100
}

variable "budget_alert_email" {
  description = "E-mail que recebe os alertas de orçamento"
  type        = string
}
