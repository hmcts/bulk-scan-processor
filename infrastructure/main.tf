provider "azurerm" {
  version = "=1.42.0"
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

  #region API gateway
  create_api = "${var.env != "preview" && var.env != "spreview"}"

  # certificate thumbprints used by API tests
  allowed_test_certificate_thumbprints = [
    "${var.api_gateway_test_valid_certificate_thumbprint}",
    "${var.api_gateway_test_expired_certificate_thumbprint}",
    "${var.api_gateway_test_not_yet_valid_certificate_thumbprint}",
  ]

  # list of the thumbprints of the SSL certificates that should be accepted by the API (gateway)
  allowed_certificate_thumbprints = "${concat(
                                         local.allowed_test_certificate_thumbprints,
                                         var.allowed_client_certificate_thumbprints
                                       )}"

  thumbprints_in_quotes     = "${formatlist("&quot;%s&quot;", local.allowed_certificate_thumbprints)}"
  thumbprints_in_quotes_str = "${join(",", local.thumbprints_in_quotes)}"
  api_policy                = "${replace(file("template/api-policy.xml"), "ALLOWED_CERTIFICATE_THUMBPRINTS", local.thumbprints_in_quotes_str)}"
  api_base_path             = "bulk-scan"

  #endregion

  sku_size                    = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"
  storage_account_name        = "${data.azurerm_key_vault_secret.storage_account_name.value}"
  storage_account_primary_key = "${data.azurerm_key_vault_secret.storage_account_primary_key.value}"
  storage_account_url_prod    = "https://bulkscan.platform.hmcts.net"
  storage_account_url_notprod = "https://bulkscan.${var.env}.platform.hmcts.net"
  storage_account_url         = "${var.env == "prod" ? local.storage_account_url_prod : local.storage_account_url_notprod}"
}

module "bulk-scan-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}-${var.component}"
  location           = "${var.location_db}"
  env                = "${var.env}"
  database_name      = "bulk_scan"
  postgresql_user    = "bulk_scanner"
  postgresql_version = "10"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = "${var.common_tags}"
  subscription       = "${var.subscription}"
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

    ERROR_NOTIFICATIONS_URL      = "${data.azurerm_key_vault_secret.notifications-url.value}"
    ERROR_NOTIFICATIONS_USERNAME = "${data.azurerm_key_vault_secret.notifications-username.value}"
    ERROR_NOTIFICATIONS_PASSWORD = "${data.azurerm_key_vault_secret.notifications-password.value}"

    S2S_URL    = "${local.s2s_url}"
    S2S_NAME   = "${var.s2s_name}"
    S2S_SECRET = "${data.azurerm_key_vault_secret.s2s_secret.value}"

    REUPLOAD_BATCH     = "${var.reupload_batch}"
    REUPLOAD_DELAY     = "${var.reupload_delay}"
    REUPLOAD_MAX_TRIES = "${var.reupload_max_tries}"
    REUPLOAD_ENABLED   = "${var.reupload_enabled}"
    SCAN_DELAY         = "${var.scan_delay}"
    SCAN_ENABLED       = "${var.scan_enabled}"

    NOTIFICATIONS_TO_ORCHESTRATOR_TASK_ENABLED = "${var.orchestrator_notifications_task_enabled}"
    NOTIFICATIONS_TO_ORCHESTRATOR_TASK_DELAY   = "${var.orchestrator_notifications_task_delay}"

    DELETE_REJECTED_FILES_ENABLED = "${var.delete_rejected_files_enabled}"
    DELETE_REJECTED_FILES_CRON    = "${var.delete_rejected_files_cron}"
    DELETE_REJECTED_FILES_TTL     = "${var.delete_rejected_files_ttl}"

    ERROR_NOTIFICATIONS_ENABLED = "${var.error_notifications_enabled}"

    STORAGE_BLOB_LEASE_TIMEOUT               = "${var.blob_lease_timeout}"               // In seconds
    STORAGE_BLOB_PROCESSING_DELAY_IN_MINUTES = "${var.blob_processing_delay_in_minutes}"

    STORAGE_BLOB_SELECTED_CONTAINER = "${var.blob_selected_container}"

    STORAGE_BLOB_SIGNATURE_ALGORITHM = "sha256withrsa"                               // none or sha256withrsa
    STORAGE_BLOB_PUBLIC_KEY          = "${var.blob_signature_verification_key_file}"

    QUEUE_ENVELOPE_SEND            = "${data.azurerm_key_vault_secret.envelopes_queue_send_conn_str.value}"
    QUEUE_NOTIFICATIONS_SEND       = "${data.azurerm_key_vault_secret.notifications_queue_send_conn_str.value}"
    QUEUE_NOTIFICATIONS_READ       = "${data.azurerm_key_vault_secret.notifications_queue_listen_conn_str.value}"
    QUEUE_PROCESSED_ENVELOPES_READ = "${data.azurerm_key_vault_secret.processed_envelopes_queue_listen_conn_str.value}"

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

data "azurerm_key_vault_secret" "envelopes_queue_send_conn_str" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "envelopes-queue-send-connection-string"
}

data "azurerm_key_vault_secret" "notifications_queue_send_conn_str" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "notifications-queue-send-connection-string"
}

data "azurerm_key_vault_secret" "notifications_queue_listen_conn_str" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "notifications-queue-listen-connection-string"
}

data "azurerm_key_vault_secret" "processed_envelopes_queue_listen_conn_str" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "processed-envelopes-queue-listen-connection-string"
}

data "azurerm_key_vault_secret" "processed_envelopes_queue_send_conn_str" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "processed-envelopes-queue-send-connection-string"
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

# region API (gateway)

data "template_file" "api_template" {
  template = "${file("${path.module}/template/api.json")}"
}

resource "azurerm_template_deployment" "api" {
  template_body       = "${data.template_file.api_template.rendered}"
  name                = "${var.product}-api-${var.env}"
  deployment_mode     = "Incremental"
  resource_group_name = "core-infra-${var.env}"
  count               = "${local.create_api ? 1 : 0}"

  parameters = {
    apiManagementServiceName = "core-api-mgmt-${var.env}"
    apiName                  = "bulk-scan-api"
    apiProductName           = "bulk-scan"
    serviceUrl               = "http://${var.product}-${var.component}-${var.env}.service.core-compute-${var.env}.internal"
    apiBasePath              = "${local.api_base_path}"
    policy                   = "${local.api_policy}"
  }
}

# endregion

# Copy postgres password for flyway migration
resource "azurerm_key_vault_secret" "flyway_password" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "flyway-password"
  value        = "${module.bulk-scan-db.postgresql_password}"
}
