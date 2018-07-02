provider "azurerm" {}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

locals {
  app                  = "bulk-scan-processor"
  is_preview           = "${(var.env == "preview" || var.env == "spreview")}"
  preview_account_name = "${var.product}bsp"
  default_account_name = "${var.product}bsp${var.env}"
  base_account_name    = "${local.is_preview ? local.preview_account_name : local.default_account_name}"
  account_name         = "${replace(local.base_account_name, "-", "")}"
  previewVaultName     = "${var.product}-bsp"
  nonPreviewVaultName  = "${var.product}-bsp-${var.env}"
  vaultName            = "${local.is_preview ? local.previewVaultName : local.nonPreviewVaultName}"

  db_connection_options  = "?ssl=true"
}

module "bulk-scan-db" {
  source              = "git@github.com:hmcts/moj-module-postgres?ref=master"
  product             = "${var.product}-${var.component}-db"
  location            = "${var.location_db}"
  env                 = "${var.env}"
  database_name       = "feature_toggle"
  postgresql_user     = "feature_toggler"
  sku_name            = "GP_Gen5_2"
  sku_tier            = "GeneralPurpose"
  common_tags         = "${var.common_tags}"
}

module "bulk-scan" {
  source       = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product      = "${var.product}-${local.app}"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend  = false
  capacity     = "${var.capacity}"
  common_tags  = "${var.common_tags}"

  app_settings = {
    // db
    BULK_SCANNING_DB_HOST         = "${module.bulk-scan-db.host_name}"
    BULK_SCANNING_DB_PORT         = "${module.bulk-scan-db.postgresql_listen_port}"
    BULK_SCANNING_DB_USER_NAME    = "${module.bulk-scan-db.user_name}"
    BULK_SCANNING_DB_PASSWORD     = "${module.bulk-scan-db.postgresql_password}"
    BULK_SCANNING_DB_NAME         = "${module.bulk-scan-db.postgresql_database}"
    BULK_SCANNING_DB_CONN_OPTIONS = "${local.db_connection_options}"
    FLYWAY_URL                    = "jdbc:postgresql://${module.bulk-scan-db.host_name}:${module.bulk-scan-db.postgresql_listen_port}/${module.bulk-scan-db.postgresql_database}${local.db_connection_options}"
    FLYWAY_USER                   = "${module.bulk-scan-db.user_name}"
    FLYWAY_PASSWORD               = "${module.bulk-scan-db.postgresql_password}"
    //
    STORAGE_ACCOUNT_NAME          = "${azurerm_storage_account.provider.name}"
    STORAGE_KEY                   = "${azurerm_storage_account.provider.primary_access_key}"
    SAS_TOKEN_VALIDITY            = "${var.token_validity}"
    // silence the "bad implementation" logs
    LOGBACK_REQUIRE_ALERT_LEVEL   = false
    LOGBACK_REQUIRE_ERROR_CODE    = false
  }
}

module "bulk-scan-key-vault" {
  source                  = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  name                    = "${local.vaultName}"
  product                 = "${var.product}"
  env                     = "${var.env}"
  tenant_id               = "${var.tenant_id}"
  object_id               = "${var.jenkins_AAD_objectId}"
  resource_group_name     = "${module.bulk-scan.resource_group_name}"
  product_group_object_id = "300e771f-856c-45cc-b899-40d78281e9c1"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"
}

resource "azurerm_storage_account" "provider" {
  name                      = "${local.account_name}"
  resource_group_name       = "${azurerm_resource_group.rg.name}"
  location                  = "${var.location}"
  account_tier              = "Standard"
  account_replication_type  = "LRS"
  account_kind              = "BlobStorage"
  enable_https_traffic_only = true
}

resource "azurerm_storage_container" "sscs" {
  name                  = "sscs"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
  storage_account_name  = "${azurerm_storage_account.provider.name}"
  container_access_type = "private"

  depends_on = ["azurerm_storage_account.provider"]
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name      = "${var.component}-POSTGRES-USER"
  value     = "${module.bulk-scan-db.user_name}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name      = "${var.component}-POSTGRES-PASS"
  value     = "${module.bulk-scan-db.postgresql_password}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name      = "${var.component}-POSTGRES-HOST"
  value     = "${module.bulk-scan-db.host_name}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name      = "${var.component}-POSTGRES-PORT"
  value     = "${module.bulk-scan-db.postgresql_listen_port}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name      = "${var.component}-POSTGRES-DATABASE"
  value     = "${module.bulk-scan-db.postgresql_database}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}
