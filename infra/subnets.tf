resource "aws_subnet" "sb_public" {
  count = 3

  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index + 1)
  availability_zone       = data.aws_availability_zones.az_available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name                     = "${local.prefixo_tag}_sb_public_${count.index + 1}"
    "kubernetes.io/role/elb" = "1"
  }
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.prefixo}-db-subnets"
  subnet_ids = aws_subnet.sb_public[*].id

  tags = {
    Name = "${local.prefixo_tag}_db_subnets"
  }
}