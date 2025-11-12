# terraform/init-databases.tf
# Script para crear bases de datos adicionales automáticamente

# Lambda function para crear las bases de datos
resource "aws_lambda_function" "init_databases" {
  filename      = "lambda/init-databases.zip"
  function_name = "arka-init-databases"
  role          = aws_iam_role.lambda_init_db.arn
  handler       = "index.handler"
  runtime       = "python3.11"
  timeout       = 300

  environment {
    variables = {
      DB_HOST     = split(":", aws_db_instance.shared.endpoint)[0]
      DB_PORT     = "5432"
      DB_USERNAME = var.db_master_username
      SECRET_ARN  = aws_secretsmanager_secret.db_master_password.arn
      DATABASES = jsonencode([
        "orderdb",
        "cartdb",
        "providerdb",
        "catalogdb",
        "shippingdb",
        "categorydb",
        "authdb"
      ])
    }
  }

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda_init_db.id]
  }

  depends_on = [aws_db_instance.shared]
}

# Security group para Lambda
resource "aws_security_group" "lambda_init_db" {
  name        = "arka-lambda-init-db-sg"
  description = "Security group for database initialization Lambda"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "arka-lambda-init-db-sg"
  }
}

# Permitir Lambda acceder a RDS
resource "aws_security_group_rule" "lambda_to_rds" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.lambda_init_db.id
  security_group_id        = aws_security_group.rds.id
}

# IAM role para Lambda
resource "aws_iam_role" "lambda_init_db" {
  name = "arka-lambda-init-db-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_init_db_basic" {
  role       = aws_iam_role.lambda_init_db.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role_policy" "lambda_init_db_secrets" {
  name = "lambda-init-db-secrets"
  role = aws_iam_role.lambda_init_db.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = aws_secretsmanager_secret.db_master_password.arn
      }
    ]
  })
}

# Trigger Lambda después de crear RDS
resource "null_resource" "trigger_init_databases" {
  triggers = {
    always_run = timestamp()
  }

  provisioner "local-exec" {
    command = <<-EOT
      aws lambda invoke \
        --function-name ${aws_lambda_function.init_databases.function_name} \
        --region ${var.aws_region} \
        response.json
    EOT
  }

  depends_on = [
    aws_lambda_function.init_databases,
    aws_db_instance.shared
  ]
}

# ===================== Lambda Code =====================
# Crear archivo localmente
resource "local_file" "lambda_init_db_code" {
  filename = "${path.module}/lambda/init-databases/index.py"
  content  = <<-PYTHON
import json
import os
import boto3
import psycopg2

def handler(event, context):
    """
    Crea bases de datos adicionales en la instancia RDS compartida
    """
    secret_arn = os.environ['SECRET_ARN']
    db_host = os.environ['DB_HOST']
    db_port = os.environ['DB_PORT']
    db_username = os.environ['DB_USERNAME']
    databases = json.loads(os.environ['DATABASES'])
    
    # Obtener password de Secrets Manager
    secrets_client = boto3.client('secretsmanager')
    secret = secrets_client.get_secret_value(SecretId=secret_arn)
    secret_data = json.loads(secret['SecretString'])
    db_password = secret_data['password']
    
    # Conectar a PostgreSQL
    conn = psycopg2.connect(
        host=db_host,
        port=db_port,
        user=db_username,
        password=db_password,
        database='inventorydb'  # Base de datos por defecto
    )
    conn.autocommit = True
    cursor = conn.cursor()
    
    results = []
    
    for db_name in databases:
        try:
            # Verificar si la base de datos existe
            cursor.execute(
                "SELECT 1 FROM pg_database WHERE datname = %s",
                (db_name,)
            )
            exists = cursor.fetchone()
            
            if not exists:
                cursor.execute(f"CREATE DATABASE {db_name}")
                results.append({
                    'database': db_name,
                    'status': 'created'
                })
                print(f"✓ Base de datos '{db_name}' creada")
            else:
                results.append({
                    'database': db_name,
                    'status': 'already_exists'
                })
                print(f"ℹ Base de datos '{db_name}' ya existe")
                
        except Exception as e:
            results.append({
                'database': db_name,
                'status': 'error',
                'error': str(e)
            })
            print(f"✗ Error creando '{db_name}': {e}")
    
    cursor.close()
    conn.close()
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'message': 'Database initialization completed',
            'results': results
        })
    }
PYTHON

  depends_on = [local_file.lambda_requirements]
}

resource "local_file" "lambda_requirements" {
  filename = "${path.module}/lambda/init-databases/requirements.txt"
  content  = "psycopg2-binary==2.9.9\nboto3==1.34.0"
}

# Empaquetar Lambda
resource "null_resource" "package_lambda" {
  triggers = {
    code_hash = local_file.lambda_init_db_code.content
  }

  provisioner "local-exec" {
    command = <<-EOT
      cd ${path.module}/lambda/init-databases
      pip install -r requirements.txt -t .
      zip -r ../init-databases.zip .
    EOT
  }

  depends_on = [
    local_file.lambda_init_db_code,
    local_file.lambda_requirements
  ]
}

data "archive_file" "lambda_init_db" {
  type        = "zip"
  source_dir  = "${path.module}/lambda/init-databases"
  output_path = "${path.module}/lambda/init-databases.zip"

  depends_on = [null_resource.package_lambda]
}