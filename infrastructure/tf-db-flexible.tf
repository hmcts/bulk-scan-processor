# Postgres 15 flexible servers
locals {
  db_host_name = "${var.product}-${var.component}-flexible-postgres-db-v15"
  db_name      = "bulk_scan"
}

module "postgresql" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                 = local.db_host_name
  product              = var.product
  component            = var.component
  location             = var.location_db
  env                  = var.env
  pgsql_admin_username = var.postgresql_user
  pgsql_databases = [
    {
      name : var.database_name
    }
  ]
  common_tags   = var.common_tags
  business_area = "cft"
  pgsql_version = "15"
  subnet_suffix = "expanded"

  admin_user_object_id = var.jenkins_AAD_objectId

  enable_schema_ownership        = true
  force_schema_ownership_trigger = "true"
  force_user_permissions_trigger = "1"
}
