terraform {
  # O bucket é criado por infra/bootstrap e informado no init:
  # terraform init -backend-config="bucket=<state_bucket>" -backend-config="region=<aws_region>"
  backend "s3" {
    key          = "jacafi/infra.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}
