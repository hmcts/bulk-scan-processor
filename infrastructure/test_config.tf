resource "azurerm_key_vault_secret" "test_storage_account_key" {
  name      = "test-storage-account-key"
  value     = "${local.storage_account_primary_key}"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "source_test_s2s_secret" {
  name      = "microservicekey-bulk-scan-processor-tests"
  vault_uri = "${local.s2s_vault_url}"
}

resource "azurerm_key_vault_secret" "test_s2s_secret" {
  name      = "test-s2s-secret"
  value     = "${data.azurerm_key_vault_secret.source_test_s2s_secret.value}"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

resource "azurerm_key_vault_secret" "s2s_secret" {
  name      = "s2s-secret"
  value     = "${data.azurerm_key_vault_secret.s2s_secret.value}"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "test_private_key_der" {
  name      = "test-private-key-der"
  vault_uri = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

// db region

output "TEST_DB_HOST" {
  sensitive = true
  value = "${module.bulk-scan-db.host_name}"
}

output "TEST_DB_PORT" {
  sensitive = true
  value = "${module.bulk-scan-db.postgresql_listen_port}"
}

output "TEST_DB_USERNAME" {
  sensitive = true
  value = "${module.bulk-scan-db.user_name}"
}

output "TEST_DB_PASSWORD" {
  sensitive = true
  value = "${module.bulk-scan-db.postgresql_password}"
}

output "TEST_DB_NAME" {
  sensitive = true
  value = "${module.bulk-scan-db.postgresql_database}"
}

output "TEST_DB_CONN_OPTIONS" {
  value = "${local.db_connection_options}"
}

// end db region
