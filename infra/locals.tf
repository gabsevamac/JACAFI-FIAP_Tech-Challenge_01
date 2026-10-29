locals {
  prefixo     = var.projeto
  prefixo_tag = replace(var.projeto, "-", "_")
}