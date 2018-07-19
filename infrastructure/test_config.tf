resource "azurerm_key_vault_secret" "test-scan-delay" {
  name      = "test-scan-delay"
  value     = "${var.scan_delay}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"

  depends_on = ["azurerm_storage_account.provider"]
}

resource "azurerm_key_vault_secret" "test-storage-account-name" {
  name      = "test-storage-account-name"
  value     = "${azurerm_storage_account.provider.name}"
  vault_uri = "${module.bulk-scan-key-vault.key_vault_uri}"

  depends_on = ["azurerm_storage_account.provider"]
}
