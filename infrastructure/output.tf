output "app_namespace" {
  value = "${var.deployment_namespace}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "vaultUri" {
  value = "${data.azurerm_key_vault.key_vault.vault_uri}"
}

output "microserviceName" {
  value = "${var.component}"
}

output "TEST_REUPLOAD_DELAY" {
  value = "${var.reupload_delay}"
}

output "TEST_SCAN_DELAY" {
  value = "${var.scan_delay}"
}

output "TEST_STORAGE_ACCOUNT_NAME" {
  sensitive = true
  value     = "${azurerm_storage_account.provider.name}"
}

output "TEST_S2S_URL" {
  value = "${local.s2s_url}"
}

output "TEST_S2S_NAME" {
  sensitive = true
  value     = "${var.test_s2s_name}"
}

output "api_gateway_url" {
  value = "https://core-api-mgmt-${var.env}.azure-api.net/${local.api_base_path}"
}

output "test_storage_container_name" {
  value = "${azurerm_storage_container.test.name}"
}

output "test_storage_account_url" {
  value = "${local.storage_account_url}"
}
