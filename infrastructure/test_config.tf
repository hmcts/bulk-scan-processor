resource "azurerm_key_vault_secret" "test-scan-delay" {
  name      = "test-scan-delay"
  value     = "${var.scan_delay}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "test-storage-account-name" {
  name      = "test-storage-account-name"
  value     = "${azurerm_storage_account.provider.name}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}

resource "azurerm_key_vault_secret" "test-storage-key" {
  name      = "test-storage-key"
  value     = "${azurerm_storage_account.provider.primary_access_key}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"
}
