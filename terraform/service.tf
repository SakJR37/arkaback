# terraform/services.tf
# Configuración automática de servicios ECS

locals {
  service_configs = {
    gateway = {
      port              = 8080
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 100
      path_pattern      = ["/*"]
      env_vars = {
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
    }
    eureka-server = {
      port              = 8761
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 99
      path_pattern      = ["/eureka/*"]
      env_vars          = {}
    }
    inventory-service = {
      port              = 8081
      cpu               = 512
      memory            = 1024
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 101
      path_pattern      = ["/api/products/*", "/api/reports/*", "/api/stock-report/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/inventorydb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
        AWS_REGION                 = var.aws_region
        S3_BUCKET_NAME            = aws_s3_bucket.reports.bucket
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    order-service = {
      port              = 8082
      cpu               = 512
      memory            = 1024
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 102
      path_pattern      = ["/api/orders/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/orderdb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
        SQS_ORDER_EVENTS_QUEUE    = aws_sqs_queue.order_events.url
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    cart-service = {
      port              = 8085
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 103
      path_pattern      = ["/api/carts/*", "/api/abandoned-carts/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/cartdb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
        SQS_ABANDONED_CART_QUEUE  = aws_sqs_queue.abandoned_cart_events.url
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    catalog-service = {
      port              = 8083
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 104
      path_pattern      = ["/api/catalog/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/catalogdb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    category-maintainer = {
      port              = 8084
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 105
      path_pattern      = ["/api/categories/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/categorydb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    provider-service = {
      port              = 8086
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 106
      path_pattern      = ["/api/providers/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/providerdb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    shipping-service = {
      port              = 8087
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 107
      path_pattern      = ["/api/shipping/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/shippingdb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    auth-service = {
      port              = 8091
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 108
      path_pattern      = ["/auth/*"]
      env_vars = {
        SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.shared.endpoint}/authdb"
        SPRING_DATASOURCE_USERNAME = var.db_master_username
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {
        SPRING_DATASOURCE_PASSWORD = aws_secretsmanager_secret.db_master_password.arn
      }
    }
    notification-service = {
      port              = 8088
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 109
      path_pattern      = ["/api/notifications/*"]
      env_vars = {
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
        SQS_NOTIFICATION_QUEUE = aws_sqs_queue.notification_events.url
        SES_FROM_EMAIL         = aws_ses_email_identity.from_email.email
      }
      secrets = {}
    }
    review-service = {
      port              = 8089
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 110
      path_pattern      = ["/api/reviews/*"]
      env_vars = {
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {}
    }
    catalog-bff-web = {
      port              = 8092
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 111
      path_pattern      = ["/api/bff/web/*"]
      env_vars = {
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {}
    }
    catalog-bff-mobile = {
      port              = 8093
      cpu               = 256
      memory            = 512
      desired_count     = 1
      health_check_path = "/actuator/health"
      priority          = 112
      path_pattern      = ["/api/bff/mobile/*"]
      env_vars = {
        EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://eureka-server.arka.local:8761/eureka/"
      }
      secrets = {}
    }
  }
}

# ===================== SERVICE DISCOVERY =====================
resource "aws_service_discovery_service" "services" {
  for_each = local.service_configs

  name = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = {
    Name = "arka-${each.key}-discovery"
  }
}

# ===================== TARGET GROUPS =====================
resource "aws_lb_target_group" "services" {
  for_each = local.service_configs

  name        = "arka-${each.key}-tg"
  port        = each.value.port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    path                = each.value.health_check_path
    matcher             = "200"
  }

  deregistration_delay = 30

  tags = {
    Name = "arka-${each.key}-tg"
  }
}

# ===================== ALB LISTENER RULES =====================
resource "aws_lb_listener_rule" "services" {
  for_each = local.service_configs

  listener_arn = aws_lb_listener.http.arn
  priority     = each.value.priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.services[each.key].arn
  }

  condition {
    path_pattern {
      values = each.value.path_pattern
    }
  }
}

# ===================== TASK DEFINITIONS =====================
resource "aws_ecs_task_definition" "services" {
  for_each = local.service_configs

  family                   = "arka-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "arka-${each.key}"
    image = "${aws_ecr_repository.services[each.key].repository_url}:latest"

    portMappings = [{
      containerPort = each.value.port
      protocol      = "tcp"
    }]

    environment = [
      for k, v in each.value.env_vars : {
        name  = k
        value = v
      }
    ]

    secrets = [
      for k, v in lookup(each.value, "secrets", {}) : {
        name      = k
        valueFrom = "${v}:password::"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.services[each.key].name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }

    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${each.value.port}${each.value.health_check_path} || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])

  tags = {
    Name = "arka-${each.key}"
  }
}

# ===================== ECS SERVICES =====================
resource "aws_ecs_service" "services" {
  for_each = local.service_configs

  name            = "arka-${each.key}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.services[each.key].arn
  desired_count   = each.value.desired_count

  launch_type = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services[each.key].arn
    container_name   = "arka-${each.key}"
    container_port   = each.value.port
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services[each.key].arn
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role.ecs_task_execution,
    aws_iam_role.ecs_task
  ]

  tags = {
    Name = "arka-${each.key}-service"
  }

  # Permitir actualizaciones sin downtime
  deployment_configuration {
    maximum_percent         = 200
    minimum_healthy_percent = 100
  }

  # Enable ECS Exec para debugging
  enable_execute_command = true
}

# ===================== AUTO SCALING =====================
resource "aws_appautoscaling_target" "services" {
  for_each = local.service_configs

  max_capacity       = 4
  min_capacity       = 1
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.services[each.key].name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "cpu" {
  for_each = local.service_configs

  name               = "arka-${each.key}-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.services[each.key].resource_id
  scalable_dimension = aws_appautoscaling_target.services[each.key].scalable_dimension
  service_namespace  = aws_appautoscaling_target.services[each.key].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_policy" "memory" {
  for_each = local.service_configs

  name               = "arka-${each.key}-memory-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.services[each.key].resource_id
  scalable_dimension = aws_appautoscaling_target.services[each.key].scalable_dimension
  service_namespace  = aws_appautoscaling_target.services[each.key].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value = 80.0
  }
}