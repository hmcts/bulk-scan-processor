data "azurerm_key_vault_secret" "notifications-url" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "error-notifications-url"
}

data "azurerm_key_vault_secret" "notifications-username" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "error-notifications-username"
}

data "azurerm_key_vault_secret" "notifications-password" {
  key_vault_id = "${data.azurerm_key_vault.key_vault.id}"
  name         = "error-notifications-password"
}
