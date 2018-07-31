output "app_namespace" {
  value = "${var.deployment_namespace}"
}

output "vaultName" {
  value = "${module.bulk-scan-key-vault.key_vault_name}"
}

output "vaultUri" {
  value = "${module.bulk-scan-key-vault.key_vault_uri}"
}

output "microserviceName" {
  value = "${var.component}"
}

output "TEST_SCAN_DELAY" {
  value = "${var.scan_delay}"
}

output "TEST_STORAGE_ACCOUNT_NAME" {
  value = "${azurerm_storage_account.provider.name}"
}

output "TEST_STORAGE_ACCOUNT_KEY" {
  value = "${azurerm_storage_account.provider.primary_access_key}"
}

# this variable will be accessible to tests as API_GATEWAY_URL environment variable
output "api_gateway_url" {
  value = "https://core-api-mgmt-${var.env}.azure-api.net/${local.api_base_path}"
}

output "test_storage_container_name" {
  value = "${azurerm_storage_container.test.name}"
}
