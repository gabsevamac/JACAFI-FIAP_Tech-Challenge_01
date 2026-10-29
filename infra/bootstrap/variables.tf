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

variable "db_instance_class" {
  description = "Única classe de RDS que o pipeline pode criar; precisa ser a mesma de infra/"
  type        = string
  default     = "db.t4g.micro"
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
