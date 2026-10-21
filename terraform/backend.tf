terraform {
  required_version = ">= 1.10.0"

  cloud {
    
    organization = "JACAFI-FIAP"

    workspaces {
      name = "mecanica-fiap-eks"
    }
  }
}