resource "aws_lb_target_group" "api" {
  name        = "${local.prefixo}-api-tg"
  port        = var.api_node_port
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = var.api_health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "${local.prefixo_tag}_api_tg"
  }
}

resource "aws_lb_target_group" "keycloak" {
  name        = "${local.prefixo}-keycloak-tg"
  port        = var.keycloak_node_port
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = aws_vpc.main.id

  health_check {
    path                = var.keycloak_health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "${local.prefixo_tag}_keycloak_tg"
  }
}

resource "aws_autoscaling_attachment" "nodes_api" {
  autoscaling_group_name = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name
  lb_target_group_arn    = aws_lb_target_group.api.arn
}

resource "aws_autoscaling_attachment" "nodes_keycloak" {
  autoscaling_group_name = aws_eks_node_group.main.resources[0].autoscaling_groups[0].name
  lb_target_group_arn    = aws_lb_target_group.keycloak.arn
}
