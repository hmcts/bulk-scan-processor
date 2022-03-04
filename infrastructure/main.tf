provider "azurerm" {
  features {}
}

locals {
  is_preview          = "${(var.env == "preview" || var.env == "spreview")}"
  account_name        = "${replace("${var.product}old${var.env}", "-", "")}"
  previewVaultName    = "${var.raw_product}-aat"
  nonPreviewVaultName = "${var.product}-${var.env}"
  vaultName           = "${local.is_preview ? local.previewVaultName : local.nonPreviewVaultName}"

  aseName   = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"

  s2s_rg       = "rpe-service-auth-provider-${local.local_env}"
  s2s_url      = "http://${local.s2s_rg}.service.core-compute-${local.local_env}.internal"
  dm_store_url = "http://dm-store-${local.local_env}.service.core-compute-${local.local_env}.internal"

  db_connection_options = "?sslmode=require"

  sku_size                    = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"
  storage_account_name        = "${data.azurerm_key_vault_secret.storage_account_name.value}"
  storage_account_primary_key = "${data.azurerm_key_vault_secret.storage_account_primary_key.value}"
  storage_account_url_prod    = "https://bulkscan.platform.hmcts.net"
  storage_account_url_notprod = "https://bulkscan.${var.env}.platform.hmcts.net"
  storage_account_url         = "${var.env == "prod" ? local.storage_account_url_prod : local.storage_account_url_notprod}"
}

module "bulk-scan-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = var.product
  component          = var.component
  location           = var.location_db
  env                = var.env
  database_name      = var.database_name
  postgresql_user    = var.postgresql_user
  postgresql_version = "10"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

module "bulk-scan-db-v11" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = var.product
  component          = var.component
  location           = var.location_db
  env                = var.env
  database_name      = var.database_name
  postgresql_user    = var.postgresql_user
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

# Staging DB to be used by AAT staging pod for functional tests
module "bulk-scan-staging-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  name               = "${var.product}-${var.component}-staging"
  product            = var.product
  component          = var.component
  location           = var.location_db
  env                = var.env
  database_name      = var.database_name
  postgresql_user    = var.postgresql_user
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

module "bulk-scan" {
  source                          = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product                         = "${var.product}-${var.component}"
  location                        = "${var.location}"
  env                             = "${var.env}"
  ilbIp                           = "${var.ilbIp}"
  subscription                    = "${var.subscription}"
  is_frontend                     = "false"
  capacity                        = "${var.capacity}"
  common_tags                     = "${var.common_tags}"
  appinsights_instrumentation_key = "${var.appinsights_instrumentation_key}"
  instance_size                   = "${local.sku_size}"
  asp_name                        = "${var.product}-${var.env}"
  asp_rg                          = "${var.product}-${var.env}"
  java_container_version          = "9.0"
  enable_ase                      = "${var.enable_ase}"

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
    FLYWAY_NOOP_STRATEGY          = "true"

    STORAGE_ACCOUNT_NAME  = "${local.storage_account_name}"
    STORAGE_KEY           = "${local.storage_account_primary_key}"
    STORAGE_URL           = "${local.storage_account_url}"
    STORAGE_PROXY_ENABLED = "${var.storage_proxy_enabled}"
    SAS_TOKEN_VALIDITY    = "${var.token_validity}"

    DOCUMENT_MANAGEMENT_URL = "${local.dm_store_url}"

    S2S_URL    = "${local.s2s_url}"
    S2S_NAME   = "${var.s2s_name}"
    S2S_SECRET = "${data.azurerm_key_vault_secret.s2s_secret.value}"

    SCAN_DELAY         = "${var.scan_delay}"
    SCAN_ENABLED       = "${var.scan_enabled}"

    NOTIFICATIONS_TO_ORCHESTRATOR_TASK_ENABLED = "${var.orchestrator_notifications_task_enabled}"
    NOTIFICATIONS_TO_ORCHESTRATOR_TASK_DELAY   = "${var.orchestrator_notifications_task_delay}"

    DELETE_REJECTED_FILES_ENABLED = "${var.delete_rejected_files_enabled}"
    DELETE_REJECTED_FILES_CRON    = "${var.delete_rejected_files_cron}"
    DELETE_REJECTED_FILES_TTL     = "${var.delete_rejected_files_ttl}"

    STORAGE_BLOB_LEASE_TIMEOUT               = "${var.blob_lease_timeout}"               // In seconds

    STORAGE_BLOB_SELECTED_CONTAINER = "${var.blob_selected_container}"

    SMTP_HOST          = "${var.smtp_host}"
    SMTP_USERNAME      = "${data.azurerm_key_vault_secret.smtp_username.value}"
    SMTP_PASSWORD      = "${data.azurerm_key_vault_secret.smtp_password.value}"
    REPORTS_CRON       = "${var.reports_cron}"
    REPORTS_RECIPIENTS = "${data.azurerm_key_vault_secret.reports_recipients.value}"

    INCOMPLETE_ENVELOPES_TASK_CRON    = "${var.incomplete_envelopes_cron}"
    INCOMPLETE_ENVELOPES_TASK_ENABLED = "${var.incomplete_envelopes_enabled}"

    PROCESS_PAYMENTS_ENABLED          = "${var.process_payments_enabled}"

    OCR_VALIDATION_URL_BULKSCAN_SAMPLE_APP = "${var.ocr_validation_url_bulkscan_sample_app}"
    OCR_VALIDATION_URL_PROBATE             = "${var.ocr_validation_url_probate}"

    NO_NEW_ENVELOPES_TASK_ENABLED     = "false"

    // silence the "bad implementation" logs
    LOGBACK_REQUIRE_ALERT_LEVEL = "false"
    LOGBACK_REQUIRE_ERROR_CODE  = "false"
  }
}

data "azurerm_key_vault" "key_vault" {
  name                = "${local.vaultName}"
  resource_group_name = "${local.vaultName}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${local.local_env}"
  resource_group_name = "${local.s2s_rg}"
}

data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = "reform-scan-${local.local_env}"
  resource_group_name = "reform-scan-${local.local_env}"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-USER"
  value        = "${module.bulk-scan-db.user_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-PASS"
  value        = "${module.bulk-scan-db.postgresql_password}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-HOST"
  value        = "${module.bulk-scan-db.host_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-PORT"
  value        = "${module.bulk-scan-db.postgresql_listen_port}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = "${module.bulk-scan-db.postgresql_database}"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER-V11" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-USER-V11"
  value        = "${module.bulk-scan-db-v11.user_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS-V11" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-PASS-V11"
  value        = "${module.bulk-scan-db-v11.postgresql_password}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST-V11" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-HOST-V11"
  value        = "${module.bulk-scan-db-v11.host_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT-V11" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-PORT-V11"
  value        = "${module.bulk-scan-db-v11.postgresql_listen_port}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE-V11" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-POSTGRES-DATABASE-V11"
  value        = "${module.bulk-scan-db-v11.postgresql_database}"
}

# region staging DB secrets
resource "azurerm_key_vault_secret" "staging_db_user" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-staging-db-user"
  value        = "${module.bulk-scan-staging-db.user_name}"
}

resource "azurerm_key_vault_secret" "staging_db_password" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-staging-db-password"
  value        = "${module.bulk-scan-staging-db.postgresql_password}"
}

resource "azurerm_key_vault_secret" "staging_db_host" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-staging-db-host"
  value        = "${module.bulk-scan-staging-db.host_name}"
}

resource "azurerm_key_vault_secret" "staging_db_port" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-staging-db-port"
  value        = "${module.bulk-scan-staging-db.postgresql_listen_port}"
}

resource "azurerm_key_vault_secret" "staging_db_name" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "${var.component}-staging-db-name"
  value        = "${module.bulk-scan-staging-db.postgresql_database}"
}
# endregion

resource "azurerm_key_vault_secret" "notifications_queue_send_access_key" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "notifications-queue-send-shared-access-key"
  value        = "${data.azurerm_key_vault_secret.notifications_queue_send_access_key.value}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = "${data.azurerm_key_vault.s2s_key_vault.id}"
  name         = "microservicekey-bulk-scan-processor"
}

data "azurerm_key_vault_secret" "storage_account_name" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "storage-account-name"
}

data "azurerm_key_vault_secret" "storage_account_primary_key" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "storage-account-primary-key"
}

data "azurerm_key_vault_secret" "notifications_queue_send_access_key" {
  key_vault_id = "${data.azurerm_key_vault.reform_scan_key_vault.id}"
  name         = "notification-queue-send-shared-access-key"
}

data "azurerm_key_vault_secret" "reports_recipients" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "reports-recipients"
}

data "azurerm_key_vault_secret" "smtp_username" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "reports-email-username"
}

data "azurerm_key_vault_secret" "smtp_password" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "reports-email-password"
}

# Copy postgres password for flyway migration
resource "azurerm_key_vault_secret" "flyway_password" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "flyway-password"
  value        = "${module.bulk-scan-db.postgresql_password}"
}
