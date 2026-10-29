resource "aws_route_table" "rt_public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${local.prefixo_tag}_rt_public"
  }
}

resource "aws_route_table_association" "rta_public" {
  count = 3

  subnet_id      = aws_subnet.sb_public[count.index].id
  route_table_id = aws_route_table.rt_public.id
}
