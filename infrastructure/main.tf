provider "azurerm" {
  features {}
}

module "bulk-scan-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}-${var.component}"
  location           = var.location_db
  env                = var.env
  database_name      = "bulk_scan"
  postgresql_user    = "bulk_scanner"
  postgresql_version = "10"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

# Staging DB to be used by AAT staging pod for functional tests
module "bulk-scan-staging-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}-${var.component}-staging"
  location           = var.location_db
  env                = var.env
  database_name      = "bulk_scan"
  postgresql_user    = "bulk_scanner"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

data "azurerm_key_vault" "key_vault" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = "reform-scan-${var.env}"
  resource_group_name = "reform-scan-${var.env}"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-POSTGRES-USER"
  value        = module.bulk-scan-db.user_name
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.bulk-scan-db.postgresql_password
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.bulk-scan-db.host_name
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-POSTGRES-PORT"
  value        = module.bulk-scan-db.postgresql_listen_port
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = module.bulk-scan-db.postgresql_database
}

# region staging DB secrets
resource "azurerm_key_vault_secret" "staging_db_user" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-staging-db-user"
  value        = module.bulk-scan-staging-db.user_name
}

resource "azurerm_key_vault_secret" "staging_db_password" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-staging-db-password"
  value        = module.bulk-scan-staging-db.postgresql_password
}

resource "azurerm_key_vault_secret" "staging_db_host" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-staging-db-host"
  value        = module.bulk-scan-staging-db.host_name
}

resource "azurerm_key_vault_secret" "staging_db_port" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-staging-db-port"
  value        = module.bulk-scan-staging-db.postgresql_listen_port
}

resource "azurerm_key_vault_secret" "staging_db_name" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "${var.component}-staging-db-name"
  value        = module.bulk-scan-staging-db.postgresql_database
}
# endregion

resource "azurerm_key_vault_secret" "notifications_queue_send_access_key" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "notifications-queue-send-shared-access-key"
  value        = data.azurerm_key_vault_secret.notifications_queue_send_access_key.value
}

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-bulk-scan-processor"
}

data "azurerm_key_vault_secret" "storage_account_primary_key" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "storage-account-primary-key"
}

data "azurerm_key_vault_secret" "notifications_queue_send_access_key" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "notification-queue-send-shared-access-key"
}

# Copy postgres password for flyway migration
resource "azurerm_key_vault_secret" "flyway_password" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "flyway-password"
  value        = module.bulk-scan-db.postgresql_password
}
