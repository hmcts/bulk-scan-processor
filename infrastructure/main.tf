locals {
  is_preview          = (var.env == "preview" || var.env == "spreview")
  account_name        = replace("${var.product}old${var.env}", "-", "")
  previewVaultName    = "${var.raw_product}-aat"
  nonPreviewVaultName = "${var.product}-${var.env}"
  vaultName           = local.is_preview ? local.previewVaultName : local.nonPreviewVaultName

  aseName   = "core-compute-${var.env}"
  local_env = (var.env == "preview" || var.env == "spreview") ? (var.env == "preview") ? "aat" : "saat" : var.env

  s2s_rg       = "rpe-service-auth-provider-${local.local_env}"
  s2s_url      = "http://${local.s2s_rg}.service.core-compute-${local.local_env}.internal"
  dm_store_url = "http://dm-store-${local.local_env}.service.core-compute-${local.local_env}.internal"

  db_connection_options = "?sslmode=require"

  sku_size                    = var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"
  storage_account_name        = data.azurerm_key_vault_secret.storage_account_name.value
  storage_account_primary_key = data.azurerm_key_vault_secret.storage_account_primary_key.value
  storage_account_url_prod    = "https://bulkscan.platform.hmcts.net"
  storage_account_url_notprod = "https://bulkscan.${var.env}.platform.hmcts.net"
  storage_account_url         = var.env == "prod" ? local.storage_account_url_prod : local.storage_account_url_notprod
}

# region: key vault definitions
data "azurerm_key_vault" "key_vault" {
  name                = local.vaultName
  resource_group_name = local.vaultName
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${local.local_env}"
  resource_group_name = local.s2s_rg
}

data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = "reform-scan-${var.env}"
  resource_group_name = "reform-scan-${var.env}"
}
# endregion

# region: notification queue access key
resource "azurerm_key_vault_secret" "notifications_queue_send_access_key" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "notifications-queue-send-shared-access-key"
  value        = data.azurerm_key_vault_secret.notifications_queue_send_access_key.value
}

resource "azurerm_key_vault_secret" "notifications_queue_send_access_key_premium" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "notifications-queue-send-shared-access-key-premium"
  value        = data.azurerm_key_vault_secret.notifications_queue_send_access_key_premium.value
}
# endregion

# region: copy s2s secret to bulk-scan key vault
data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-bulk-scan-processor"
}
# endregion

# region: storage secrets
data "azurerm_key_vault_secret" "storage_account_name" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "storage-account-name"
}

data "azurerm_key_vault_secret" "storage_account_primary_key" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "storage-account-primary-key"
}
# endregion

# region: copy notification queue secrets to bulk-scan key vault
data "azurerm_key_vault_secret" "notifications_queue_send_access_key" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "notification-queue-send-shared-access-key-premium"
}

data "azurerm_key_vault_secret" "notifications_queue_send_access_key_premium" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "notification-queue-send-shared-access-key-premium"
}
# endregion

# region: reports secrets
data "azurerm_key_vault_secret" "reports_recipients" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "reports-recipients"
}

data "azurerm_key_vault_secret" "smtp_username" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "reports-email-username"
}

data "azurerm_key_vault_secret" "smtp_password" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "reports-email-password"
}
# endregion

# region launchdarkly secrets

data "azurerm_key_vault_secret" "launch_darkly_sdk_key" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "launch-darkly-sdk-key"
}

data "azurerm_key_vault_secret" "launch_darkly_offline_mode" {
  key_vault_id = data.azurerm_key_vault.key_vault.id
  name         = "launch-darkly-offline-mode"
}

# endregion
