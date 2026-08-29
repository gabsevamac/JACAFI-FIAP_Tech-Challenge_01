variable "cluster_name" {
  description = "Name of the local Kind cluster."
  type        = string
  default     = "jacafi-tech"
}

variable "database_name" {
  description = "Database created by PostgreSQL."
  type        = string
  default     = "jacafi"
}

variable "database_username" {
  description = "Database username."
  type        = string
  sensitive   = true
}

variable "database_password" {
  description = "Database password."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "HS256 key with at least 32 bytes."
  type        = string
  sensitive   = true
}
