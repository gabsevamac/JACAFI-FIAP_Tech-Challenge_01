output "aws_region" {
  description = "Variável AWS_REGION do GitHub"
  value       = var.aws_region
}

output "aws_role_arn" {
  description = "Variável AWS_ROLE_ARN do GitHub"
  value       = aws_iam_role.github_actions.arn
}

output "tf_state_bucket" {
  description = "Variável TF_STATE_BUCKET do GitHub"
  value       = aws_s3_bucket.tfstate.bucket
}

output "ecr_repository_url" {
  description = "Variável ECR_REPOSITORY_URL do GitHub"
  value       = aws_ecr_repository.api.repository_url
}
