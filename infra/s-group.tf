resource "aws_security_group" "alb" {
  name   = "${local.prefixo}-alb-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    description = "HTTP da API"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP do Keycloak"
    from_port   = var.keycloak_listener_port
    to_port     = var.keycloak_listener_port
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.prefixo_tag}_alb_sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.prefixo}-rds-sg"
  description = "PostgreSQL acessivel apenas pelo cluster EKS"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${local.prefixo_tag}_rds_sg"
  }
}
