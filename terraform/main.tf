terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
  
  backend "s3" {
    bucket  = "arkamodulesk3"
    key     = "arka/terraform.tfstate"
    region  = "us-east-2"
    encrypt = true
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "Arka"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# ===================== VARIABLES =====================
variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_master_username" {
  description = "Master username for RDS"
  type        = string
  default     = "postgres"
}

variable "github_repo" {
  description = "GitHub repository for CI/CD"
  type        = string
  default     = "your-org/arka-backend"
}

# ===================== SECRETS =====================
variable "db_password" {
  description = "Master password for RDS"
  type        = string
  sensitive   = true
  default     = "Arka$123"
}
# Store DB password in Secrets Manager
resource "aws_secretsmanager_secret" "db_master_password" {
  name_prefix             = "arka/db/master-password-"
  description             = "Master password for Arka RDS instances"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "db_master_password" {
  secret_id = aws_secretsmanager_secret.db_master_password.id
  secret_string = jsonencode({
    username = var.db_master_username
    password = var.db_password
  })
}

# ===================== VPC & NETWORKING =====================
data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "arka-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "arka-igw"
  }
}

# Public Subnets (for ALB, NAT Gateway)
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "arka-public-subnet-${count.index + 1}"
    Type = "public"
  }
}

# Private Subnets (for ECS Tasks)
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "arka-private-subnet-${count.index + 1}"
    Type = "private"
  }
}

# Database Subnets (isolated)
resource "aws_subnet" "database" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 20)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "arka-database-subnet-${count.index + 1}"
    Type = "database"
  }
}

# NAT Gateway
resource "aws_eip" "nat" {
  domain = "vpc"
  
  tags = {
    Name = "arka-nat-eip"
  }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "arka-nat-gateway"
  }
  
  depends_on = [aws_internet_gateway.main]
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "arka-public-rt"
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "arka-private-rt"
  }
}

resource "aws_route_table" "database" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "arka-database-rt"
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "database" {
  count          = 2
  subnet_id      = aws_subnet.database[count.index].id
  route_table_id = aws_route_table.database.id
}

# ===================== SECURITY GROUPS =====================
resource "aws_security_group" "alb" {
  name        = "arka-alb-sg"
  description = "Security group for Application Load Balancer"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
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
    Name = "arka-alb-sg"
  }
}

resource "aws_security_group" "ecs" {
  name        = "arka-ecs-sg"
  description = "Security group for ECS tasks"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "All traffic from ALB"
    from_port       = 0
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "Microservices communication"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "arka-ecs-sg"
  }
}

resource "aws_security_group" "rds" {
  name        = "arka-rds-sg"
  description = "Security group for RDS databases"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "arka-rds-sg"
  }
}
resource "aws_security_group_rule" "rds_public_access" {
  type              = "ingress"
  from_port         = 5432
  to_port           = 5432
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.rds.id
  description       = "Allow PostgreSQL access from anywhere"
}
# ===================== RDS DATABASES =====================
# DB Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "arka-db-subnet-group"
  subnet_ids = aws_subnet.database[*].id

  tags = {
    Name = "arka-db-subnet-group"
  }
}

# One RDS instance with multiple databases (Free Tier compliant)
resource "aws_db_instance" "shared" {
  identifier     = "arka-shared-db"
  engine         = "postgres"
  engine_version = "14.13"
  
  # Free Tier
  instance_class        = "db.t3.micro"
  allocated_storage     = 20
  max_allocated_storage = 0  # Disable autoscaling for free tier
  storage_type          = "gp2"
  storage_encrypted     = true
  
  # Database
  db_name  = "inventorydb"  # Default database
  username = var.db_master_username
  password = var.db_password
  port     = 5432
  
  # Network
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = true
  
  # Backup
  backup_retention_period   = 7
  backup_window             = "03:00-04:00"
  maintenance_window        = "mon:04:00-mon:05:00"
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  # Performance
  multi_az               = false  # Free tier doesn't support multi-AZ
  auto_minor_version_upgrade = true
  
  # Deletion protection
  deletion_protection = false
  skip_final_snapshot = true
  
  # Parameter group
  parameter_group_name = aws_db_parameter_group.postgres14.name
  
  tags = {
    Name = "arka-shared-db"
  }
}

resource "aws_db_parameter_group" "postgres14" {
  name   = "arka-postgres14"
  family = "postgres14"

  parameter {
    name  = "log_connections"
    value = "1"
  }

  parameter {
    name  = "log_disconnections"
    value = "1"
  }

  tags = {
    Name = "arka-postgres14-params"
  }
}

# ===================== S3 BUCKETS =====================
# Reports bucket
resource "aws_s3_bucket" "reports" {
  bucket = "arka-reports-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name = "arka-reports"
  }
}

resource "aws_s3_bucket_versioning" "reports" {
  bucket = aws_s3_bucket.reports.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "reports" {
  bucket = aws_s3_bucket.reports.id

  rule {
    id     = "delete-old-reports"
    status = "Enabled"

    filter {}  # Agregar esta línea

    expiration {
      days = 90
    }
  }
}

# ===================== SQS QUEUES =====================
resource "aws_sqs_queue" "order_events" {
  name                      = "arka-order-events"
  delay_seconds             = 0
  max_message_size          = 262144
  message_retention_seconds = 345600  # 4 days
  receive_wait_time_seconds = 10

  tags = {
    Name = "arka-order-events"
  }
}

resource "aws_sqs_queue" "order_events_dlq" {
  name = "arka-order-events-dlq"

  tags = {
    Name = "arka-order-events-dlq"
  }
}

resource "aws_sqs_queue_redrive_policy" "order_events" {
  queue_url = aws_sqs_queue.order_events.id

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.order_events_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_sqs_queue" "notification_events" {
  name                      = "arka-notification-events"
  delay_seconds             = 0
  max_message_size          = 262144
  message_retention_seconds = 345600
  receive_wait_time_seconds = 10

  tags = {
    Name = "arka-notification-events"
  }
}

resource "aws_sqs_queue" "abandoned_cart_events" {
  name                      = "arka-abandoned-cart-events"
  delay_seconds             = 0
  max_message_size          = 262144
  message_retention_seconds = 345600
  receive_wait_time_seconds = 10

  tags = {
    Name = "arka-abandoned-cart-events"
  }
}

# ===================== SES (Email) =====================
resource "aws_ses_email_identity" "from_email" {
  email = "noreply@yourdomain.com"  # Change this
}

resource "aws_ses_configuration_set" "main" {
  name = "arka-email-config"
}

# ===================== ECR REPOSITORIES =====================
locals {
  services = [
    "auth-service",
    "cart-service",
    "catalog-service",
    "inventory-service",
    "order-service",
    "notification-service",
    "review-service",
    "shipping-service",
    "provider-service",
    "category-maintainer",
    "catalog-bff-web",
    "catalog-bff-mobile",
    "eureka-server",
    "gateway"
  ]
}

resource "aws_ecr_repository" "services" {
  for_each = toset(local.services)
  
  name                 = "arka-${each.key}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "arka-${each.key}"
  }
}

resource "aws_ecr_lifecycle_policy" "services" {
  for_each = aws_ecr_repository.services

  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection = {
        tagStatus     = "any"
        countType     = "imageCountMoreThan"
        countNumber   = 5
      }
      action = {
        type = "expire"
      }
    }]
  })
}

# ===================== ECS CLUSTER =====================
resource "aws_ecs_cluster" "main" {
  name = "arka-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "arka-cluster"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    base              = 1
    weight            = 100
    capacity_provider = "FARGATE"
  }
}

# ===================== SERVICE DISCOVERY (Cloud Map) =====================
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "arka.local"
  description = "Service discovery for Arka microservices"
  vpc         = aws_vpc.main.id

  tags = {
    Name = "arka-service-discovery"
  }
}

# ===================== IAM ROLES =====================
data "aws_caller_identity" "current" {}

resource "aws_iam_role" "ecs_task_execution" {
  name = "arkaEcsTaskExecutionRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Additional policy for Secrets Manager and SSM
resource "aws_iam_role_policy" "ecs_task_execution_secrets" {
  name = "ecs-task-execution-secrets"
  role = aws_iam_role.ecs_task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "ssm:GetParameters",
          "kms:Decrypt"
        ]
        Resource = [
          aws_secretsmanager_secret.db_master_password.arn,
          "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/arka/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role" "ecs_task" {
  name = "arkaEcsTaskRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

# Task role policies (for application needs)
resource "aws_iam_role_policy" "ecs_task_policies" {
  name = "ecs-task-policies"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.reports.arn,
          "${aws_s3_bucket.reports.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = [
          aws_sqs_queue.order_events.arn,
          aws_sqs_queue.notification_events.arn,
          aws_sqs_queue.abandoned_cart_events.arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "servicediscovery:DiscoverInstances",
          "servicediscovery:ListServices",
          "servicediscovery:ListInstances"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "*"
      }
    ]
  })
}

# ===================== CLOUDWATCH LOG GROUPS =====================
resource "aws_cloudwatch_log_group" "services" {
  for_each = toset(local.services)
  
  name              = "/ecs/arka-${each.key}"
  retention_in_days = 7

  tags = {
    Name = "arka-${each.key}-logs"
  }
}

# ===================== APPLICATION LOAD BALANCER =====================
resource "aws_lb" "main" {
  name               = "arka-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = false
  enable_http2              = true

  tags = {
    Name = "arka-alb"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Arka API - No route configured"
      status_code  = "404"
    }
  }
}

# ===================== OUTPUTS =====================
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "database_subnet_ids" {
  description = "Database subnet IDs"
  value       = aws_subnet.database[*].id
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.main.dns_name
}

output "alb_url" {
  description = "ALB URL"
  value       = "http://${aws_lb.main.dns_name}"
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.shared.endpoint
}

output "rds_password_secret_arn" {
  description = "ARN of the secret containing RDS password"
  value       = aws_secretsmanager_secret.db_master_password.arn
  sensitive   = true
}

output "ecr_repositories" {
  description = "ECR repository URLs"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "s3_reports_bucket" {
  description = "S3 bucket for reports"
  value       = aws_s3_bucket.reports.bucket
}

output "sqs_queues" {
  description = "SQS queue URLs"
  value = {
    order_events          = aws_sqs_queue.order_events.url
    notification_events   = aws_sqs_queue.notification_events.url
    abandoned_cart_events = aws_sqs_queue.abandoned_cart_events.url
  }
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "service_discovery_namespace" {
  description = "Cloud Map namespace"
  value       = aws_service_discovery_private_dns_namespace.main.name
}

output "ecs_task_execution_role_arn" {
  description = "ECS task execution role ARN"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "ecs_task_role_arn" {
  description = "ECS task role ARN"
  value       = aws_iam_role.ecs_task.arn
}

output "ecs_security_group_id" {
  description = "ECS security group ID"
  value       = aws_security_group.ecs.id
}

output "alb_listener_arn" {
  description = "ALB HTTP listener ARN"
  value       = aws_lb_listener.http.arn
}

# ===================== CREDENCIALES DE BASE DE DATOS =====================
output "database_credentials" {
  description = "Database connection information"
  value = {
    host     = split(":", aws_db_instance.shared.endpoint)[0]
    port     = aws_db_instance.shared.port
    username = var.db_master_username
    password = var.db_password
    default_database = "inventorydb"
    all_databases = [
      "inventorydb",
      "orderdb",
      "cartdb",
      "providerdb",
      "catalogdb",
      "shippingdb",
      "categorydb",
      "authdb"
    ]
  }
  sensitive = true
}

output "pgadmin_connection_string" {
  description = "Connection string for pgAdmin"
  value       = "postgresql://${var.db_master_username}:${var.db_password}@${split(":", aws_db_instance.shared.endpoint)[0]}:5432/inventorydb"
  sensitive   = true
}
#wawa