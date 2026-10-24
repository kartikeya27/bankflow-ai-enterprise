resource "aws_ecr_repository" "app" {
  name = var.project
  image_scanning_configuration { scan_on_push = true }
  image_tag_mutability = "IMMUTABLE"
}
resource "aws_security_group" "alb" {
  name_prefix = "${var.project}-alb-"
  vpc_id = var.vpc_id
  ingress { from_port=443 to_port=443 protocol="tcp" cidr_blocks=["0.0.0.0/0"] }
  egress { from_port=0 to_port=0 protocol="-1" cidr_blocks=["0.0.0.0/0"] }
}
resource "aws_security_group" "ecs" {
  name_prefix = "${var.project}-ecs-"
  vpc_id=var.vpc_id
  ingress { from_port=8080 to_port=8080 protocol="tcp" security_groups=[aws_security_group.alb.id] }
  egress { from_port=0 to_port=0 protocol="-1" cidr_blocks=["0.0.0.0/0"] }
}
resource "aws_security_group" "db" {
  name_prefix = "${var.project}-db-"
  vpc_id=var.vpc_id
  ingress { from_port=5432 to_port=5432 protocol="tcp" security_groups=[aws_security_group.ecs.id] }
  egress { from_port=0 to_port=0 protocol="-1" cidr_blocks=["0.0.0.0/0"] }
}
resource "aws_db_subnet_group" "main" { name="${var.project}-db" subnet_ids=var.private_subnets }
resource "random_password" "db" { length=32 special=false }
resource "aws_secretsmanager_secret" "db_password" { name_prefix="${var.project}-db-pass-" recovery_window_in_days=7 }
resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id=aws_secretsmanager_secret.db_password.id
  secret_string=random_password.db.result
}
resource "aws_db_instance" "postgres" {
  identifier=var.project
  engine="postgres"
  engine_version="17"
  instance_class=var.db_instance_class
  allocated_storage=var.db_allocated_storage
  max_allocated_storage=100
  db_name="bankflow"
  username="bankflow"
  password=random_password.db.result
  db_subnet_group_name=aws_db_subnet_group.main.name
  vpc_security_group_ids=[aws_security_group.db.id]
  publicly_accessible=false
  storage_encrypted=true
  backup_retention_period=7
  deletion_protection=true
  skip_final_snapshot=false
  final_snapshot_identifier="${var.project}-final"
  multi_az=false # Demo cost setting. Enable Multi-AZ for production availability.
}
resource "aws_cloudwatch_log_group" "app" { name="/ecs/${var.project}" retention_in_days=30 }
resource "aws_iam_role" "execution" {
  name="${var.project}-execution"
  assume_role_policy=jsonencode({Version="2012-10-17",Statement=[{Effect="Allow",Principal={Service="ecs-tasks.amazonaws.com"},Action="sts:AssumeRole"}]})
}
resource "aws_iam_role_policy_attachment" "execution" {
  role=aws_iam_role.execution.name
  policy_arn="arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}
resource "aws_iam_role_policy" "execution_secrets" {
  name="${var.project}-secrets"
  role=aws_iam_role.execution.id
  policy=jsonencode({Version="2012-10-17",Statement=[{
    Effect="Allow",Action=["secretsmanager:GetSecretValue"],
    Resource=[aws_secretsmanager_secret.db_password.arn,var.core_sandbox_key_secret_arn]
  }]})
}
resource "aws_iam_role" "task" {
  name="${var.project}-task"
  assume_role_policy=jsonencode({Version="2012-10-17",Statement=[{Effect="Allow",Principal={Service="ecs-tasks.amazonaws.com"},Action="sts:AssumeRole"}]})
}
resource "aws_iam_role_policy" "bedrock" {
  name="${var.project}-bedrock"
  role=aws_iam_role.task.id
  policy=jsonencode({Version="2012-10-17",Statement=[{
    Effect="Allow",Action=["bedrock:InvokeModel"],Resource=[
      "arn:aws:bedrock:${var.aws_region}::foundation-model/${var.bedrock_model_id}",
      "arn:aws:bedrock:${var.aws_region}::foundation-model/${var.embedding_model_id}"
    ]
  }]})
}
resource "aws_ecs_cluster" "main" { name=var.project }
resource "aws_lb" "main" {
  name="bankflow-ai-alb" internal=false load_balancer_type="application"
  security_groups=[aws_security_group.alb.id] subnets=var.public_subnets
}
resource "aws_lb_target_group" "app" {
  name="bankflow-ai-tg" port=8080 protocol="HTTP" target_type="ip" vpc_id=var.vpc_id
  health_check { path="/actuator/health" matcher="200" interval=30 }
}
resource "aws_lb_listener" "https" {
  load_balancer_arn=aws_lb.main.arn port=443 protocol="HTTPS"
  ssl_policy="ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn=var.alb_certificate_arn
  default_action { type="forward" target_group_arn=aws_lb_target_group.app.arn }
}
resource "aws_ecs_task_definition" "app" {
  family=var.project
  network_mode="awsvpc"
  requires_compatibilities=["FARGATE"]
  cpu="512" memory="1024"
  execution_role_arn=aws_iam_role.execution.arn
  task_role_arn=aws_iam_role.task.arn
  container_definitions=jsonencode([{
    name="bankflow",image="${aws_ecr_repository.app.repository_url}:${var.container_tag}",essential=true,
    portMappings=[{containerPort=8080,hostPort=8080,protocol="tcp"}],
    environment=[
      {name="SPRING_PROFILES_ACTIVE",value="prod"},
      {name="HOST",value="0.0.0.0"},
      {name="DB_URL",value="jdbc:postgresql://${aws_db_instance.postgres.address}:5432/bankflow?sslmode=require"},
      {name="DB_USER",value="bankflow"},
      {name="AI_MODE",value="bedrock"},
      {name="EMBEDDING_MODE",value="bedrock"},
      {name="CORE_MODE",value="http"},
      {name="CORE_URL",value=var.core_sandbox_url},
      {name="OIDC_ISSUER_URI",value=var.oidc_issuer_uri},
      {name="AWS_REGION",value=var.aws_region},
      {name="BEDROCK_MODEL_ID",value=var.bedrock_model_id},
      {name="EMBEDDING_MODEL_ID",value=var.embedding_model_id}
    ],
    secrets=[
      {name="DB_PASSWORD",valueFrom=aws_secretsmanager_secret.db_password.arn},
      {name="CORE_API_KEY",valueFrom=var.core_sandbox_key_secret_arn}
    ],
    logConfiguration={logDriver="awslogs",options={
      "awslogs-group"=aws_cloudwatch_log_group.app.name,
      "awslogs-region"=var.aws_region,
      "awslogs-stream-prefix"="ecs"
    }}
  }])
}
resource "aws_ecs_service" "app" {
  name=var.project
  cluster=aws_ecs_cluster.main.id
  task_definition=aws_ecs_task_definition.app.arn
  desired_count=1
  launch_type="FARGATE"
  network_configuration {
    subnets=var.private_subnets
    security_groups=[aws_security_group.ecs.id]
    assign_public_ip=false
  }
  load_balancer { target_group_arn=aws_lb_target_group.app.arn container_name="bankflow" container_port=8080 }
  depends_on=[aws_lb_listener.https,aws_iam_role_policy.execution_secrets]
}

resource "aws_route53_record" "app" {
  zone_id=var.route53_zone_id
  name=var.app_hostname
  type="A"
  alias {
    name=aws_lb.main.dns_name
    zone_id=aws_lb.main.zone_id
    evaluate_target_health=true
  }
}
