# Guía de Configuración AWS ECS Fargate - Arka Microservices

## 📋 Prerequisitos

1. Cuenta AWS activa
2. AWS CLI instalado y configurado
3. Repositorio GitHub conectado
4. Secretos configurados en GitHub

## 🔐 Secretos de GitHub (Settings → Secrets and variables → Actions)

Agrega estos secretos:

```
AWS_ACCOUNT_ID=123456789012
AWS_ACCESS_KEY_ID=AKIAXXXXXXXXXXXXXXXX
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

## 🏗️ Infraestructura AWS Necesaria

### 1. Crear ECR Repositories (una vez)

```bash
#!/bin/bash
# Script: create-ecr-repos.sh

SERVICES=(
  "auth-service"
  "cart-service"
  "catalog-service"
  "inventory-service"
  "order-service"
  "notification-service"
  "review-service"
  "shipping-service"
  "provider-service"
  "category-maintainer"
  "eureka-server"
  "gateway"
)

for service in "${SERVICES[@]}"; do
  echo "Creating ECR repository: arka-$service"
  aws ecr create-repository \
    --repository-name arka-$service \
    --region us-east-1 \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
done
```

### 2. Crear VPC y Subnets (usando CloudFormation o Terraform)

```yaml
# cloudformation-vpc.yml
AWSTemplateFormatVersion: '2010-09-09'
Description: VPC for Arka Microservices

Resources:
  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: 10.0.0.0/16
      EnableDnsHostnames: true
      EnableDnsSupport: true
      Tags:
        - Key: Name
          Value: arka-vpc

  PublicSubnet1:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref VPC
      CidrBlock: 10.0.1.0/24
      AvailabilityZone: !Select [0, !GetAZs '']
      MapPublicIpOnLaunch: true
      Tags:
        - Key: Name
          Value: arka-public-subnet-1

  PublicSubnet2:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref VPC
      CidrBlock: 10.0.2.0/24
      AvailabilityZone: !Select [1, !GetAZs '']
      MapPublicIpOnLaunch: true
      Tags:
        - Key: Name
          Value: arka-public-subnet-2

  InternetGateway:
    Type: AWS::EC2::InternetGateway
    Properties:
      Tags:
        - Key: Name
          Value: arka-igw

  AttachGateway:
    Type: AWS::EC2::VPCGatewayAttachment
    Properties:
      VpcId: !Ref VPC
      InternetGatewayId: !Ref InternetGateway

  PublicRouteTable:
    Type: AWS::EC2::RouteTable
    Properties:
      VpcId: !Ref VPC
      Tags:
        - Key: Name
          Value: arka-public-rt

  PublicRoute:
    Type: AWS::EC2::Route
    DependsOn: AttachGateway
    Properties:
      RouteTableId: !Ref PublicRouteTable
      DestinationCidrBlock: 0.0.0.0/0
      GatewayId: !Ref InternetGateway

  SubnetRouteTableAssociation1:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      SubnetId: !Ref PublicSubnet1
      RouteTableId: !Ref PublicRouteTable

  SubnetRouteTableAssociation2:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      SubnetId: !Ref PublicSubnet2
      RouteTableId: !Ref PublicRouteTable

Outputs:
  VPCId:
    Value: !Ref VPC
    Export:
      Name: arka-vpc-id
  
  PublicSubnet1Id:
    Value: !Ref PublicSubnet1
    Export:
      Name: arka-public-subnet-1
  
  PublicSubnet2Id:
    Value: !Ref PublicSubnet2
    Export:
      Name: arka-public-subnet-2
```

Deploy VPC:
```bash
aws cloudformation create-stack \
  --stack-name arka-vpc \
  --template-body file://cloudformation-vpc.yml \
  --region us-east-1
```

### 3. Crear ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name arka-cluster \
  --region us-east-1 \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE_SPOT,weight=1
```

### 4. Crear Task Definitions y Services (Script automatizado)

```bash
#!/bin/bash
# Script: create-ecs-services.sh

CLUSTER="arka-cluster"
REGION="us-east-1"
ACCOUNT_ID="886452736463"  # Tu AWS Account ID

# Array de servicios con puertos
declare -A SERVICES=(
  ["auth-service"]="8091"
  ["cart-service"]="8092"
  ["catalog-service"]="8093"
  ["inventory-service"]="8094"
  ["order-service"]="8095"
  ["notification-service"]="8096"
  ["review-service"]="8097"
  ["shipping-service"]="8098"
  ["provider-service"]="8099"
  ["category-maintainer"]="8100"
  ["eureka-server"]="8761"
  ["gateway"]="8080"
)

for service in "${!SERVICES[@]}"; do
  port="${SERVICES[$service]}"
  
  echo "Creating task definition for $service..."
  
  cat > task-def-$service.json <<EOF
{
  "family": "arka-$service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "containerDefinitions": [
    {
      "name": "arka-$service",
      "image": "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/arka-$service:latest",
      "portMappings": [
        {
          "containerPort": $port,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/arka-$service",
          "awslogs-region": "$REGION",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ],
  "executionRoleArn": "arn:aws:iam::$ACCOUNT_ID:role/ecsTaskExecutionRole"
}
EOF

  aws ecs register-task-definition \
    --cli-input-json file://task-def-$service.json \
    --region $REGION

  echo "Creating service for $service..."
  
  aws ecs create-service \
    --cluster $CLUSTER \
    --service-name arka-$service-service \
    --task-definition arka-$service \
    --desired-count 1 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" \
    --region $REGION
    
  rm task-def-$service.json
done
```

### 5. Crear Lambda Functions

```bash
#!/bin/bash
# Script: create-lambdas.sh

LAMBDAS=("abandoned-cart-checker" "shipping-delivery" "shipping-express" "shipping-pickup")
ROLE_ARN="arn:aws:iam::123456789012:role/lambda-execution-role"

for lambda in "${LAMBDAS[@]}"; do
  cd lambda/$lambda
  npm install --production
  zip -r function.zip .
  
  aws lambda create-function \
    --function-name arka-$lambda \
    --runtime nodejs18.x \
    --role $ROLE_ARN \
    --handler index.handler \
    --zip-file fileb://function.zip \
    --timeout 30 \
    --memory-size 256 \
    --region us-east-1
  
  rm function.zip
  cd ../..
done
```

### 6. Crear SQS Queue y SNS Topic

```bash
# SQS Queue
aws sqs create-queue \
  --queue-name arka-abandoned-carts \
  --region us-east-1

# SNS Topic
aws sns create-topic \
  --name arka-notifications \
  --region us-east-1

# Subscribe notification-service endpoint
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:123456789012:arka-notifications \
  --protocol http \
  --notification-endpoint http://your-notification-service-url/sns/webhook
```

## 💰 Estimación de Costos Mensuales (región us-east-1)

### ECS Fargate (12 servicios con configuración mínima)
- **CPU**: 256 (0.25 vCPU) x 12 servicios
- **Memoria**: 512 MB x 12 servicios
- **Costo por hora**: ~$0.01268/hora por tarea
- **Costo mensual**: 12 servicios x $0.01268 x 730 horas = **~$111/mes**

### Lambda (estimación baja - 1M invocaciones/mes)
- **Costo**: ~$0.20/mes por función x 4 = **~$0.80/mes**

### ECR (almacenamiento de imágenes Docker)
- **Costo**: ~$0.10/GB/mes x 10GB = **~$1/mes**

### CloudWatch Logs
- **Costo**: ~$5/mes

### Application Load Balancer (opcional)
- **Costo**: ~$16/mes + $0.008/LCU-hora

### **TOTAL ESTIMADO: ~$120-140/mes** (sin ALB, con tráfico bajo)

## 🚀 Para reducir costos:

1. **Usar Fargate Spot** (ya configurado en CI/CD) - **ahorro 70%**
2. **Reducir a 128 CPU / 256 MB** para servicios ligeros
3. **Auto-scaling** basado en horarios (apagar servicios de noche/fines de semana)
4. **Consolidar servicios** similares en un solo contenedor

```bash
# Ejemplo: Configurar auto-scaling para reducir a 0 réplicas de noche
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/arka-cluster/arka-auth-service \
  --min-capacity 0 \
  --max-capacity 2
```

## 📊 Monitoreo

Accede a CloudWatch para ver logs y métricas:
```bash
# Ver logs de un servicio
aws logs tail /ecs/arka-auth-service --follow --region us-east-1
```

## 🔄 Deployment Automático

El pipeline de GitHub Actions se ejecutará automáticamente en cada push a `main`, ejecutando:
1. Tests de todos los servicios
2. Build de imágenes Docker
3. Push a ECR
4. Deploy a ECS Fargate
5. Deploy de Lambdas

¡Listo! 🎉
