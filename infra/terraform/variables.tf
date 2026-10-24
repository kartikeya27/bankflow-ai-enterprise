variable "aws_region" { type = string, default = "us-east-1" }
variable "project" { type = string, default = "bankflow-ai" }
variable "vpc_id" { type = string, description = "Existing VPC with outbound routing" }
variable "public_subnets" { type = list(string), description = "At least two subnets for public ALB" }
variable "private_subnets" { type = list(string), description = "At least two private subnets with NAT or VPC endpoints" }
variable "alb_certificate_arn" { type = string, description = "Existing ACM TLS certificate ARN" }
variable "oidc_issuer_uri" { type = string, description = "Trusted OIDC issuer HTTPS URL" }
variable "app_hostname" { type = string, description = "Custom HTTPS hostname matching the ACM certificate" }
variable "route53_zone_id" { type = string, description = "Existing Route53 public hosted zone ID" }
variable "core_sandbox_url" { type = string, description = "Approved sandbox HTTPS API (never payment execution)" }
variable "core_sandbox_key_secret_arn" { type = string, description = "Pre-created Secrets Manager secret with sandbox API key" }
variable "container_tag" { type = string, default = "v2" }
variable "bedrock_model_id" { type = string, default = "amazon.nova-lite-v1:0" }
variable "embedding_model_id" { type = string, default = "amazon.titan-embed-text-v2:0" }
variable "db_instance_class" { type = string, default = "db.t4g.micro" }
variable "db_allocated_storage" { type = number, default = 20 }
