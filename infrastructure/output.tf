output "app_namespace" {
  value = "${var.deployment_namespace}"
}

output "vaultName" {
  value = "${local.vaultName}"
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
