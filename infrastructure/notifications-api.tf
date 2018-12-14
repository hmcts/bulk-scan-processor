data "azurerm_key_vault_secret" "notifications-url" {
  name = "error-notifications-url"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "notifications-username" {
  name = "error-notifications-username"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "notifications-password" {
  name = "error-notifications-password"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}
