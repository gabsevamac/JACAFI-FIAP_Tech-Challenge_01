resource "aws_lb" "main" {
  name               = "${local.prefixo}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.sb_public[*].id

  depends_on = [aws_route_table_association.rta_public]

  tags = {
    Name = "${local.prefixo_tag}_alb"
  }
}