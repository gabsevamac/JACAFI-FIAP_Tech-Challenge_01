output "aws_region" {
  description = "Variável AWS_REGION do GitHub"
  value       = var.aws_region
}

output "pipeline_user_name" {
  description = "Usuário IAM do pipeline; a access key dele vai para os secrets do repositório no GitHub"
  value       = aws_iam_user.pipeline.name
}

output "tf_state_bucket" {
  description = "Variável TF_STATE_BUCKET do GitHub"
  value       = aws_s3_bucket.tfstate.bucket
}

output "ecr_repository_url" {
  description = "Variável ECR_REPOSITORY_URL do GitHub"
  value       = aws_ecr_repository.api.repository_url
}
