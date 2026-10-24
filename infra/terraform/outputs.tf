output "ecr_repository_url" { value=aws_ecr_repository.app.repository_url }
output "alb_url" { value="https://${var.app_hostname}" }
output "rds_hostname" { value=aws_db_instance.postgres.address }
