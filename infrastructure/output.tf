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
  value = "${local.storage_account_name}"
}

output "TEST_S2S_URL" {
  value = "${local.s2s_url}"
}

output "TEST_S2S_NAME" {
  sensitive = "true"
  value     = "${var.test_s2s_name}"
}

output "TEST_STORAGE_ACCOUNT_URL" {
  value = "${local.storage_account_url}"
}

output "PROCESSED_ENVELOPES_QUEUE_WRITE_CONN_STRING" {
  value = "${data.terraform_remote_state.shared_infra.processed_envelopes_queue_primary_send_connection_string}"
}

output "api_gateway_url" {
  value = "https://core-api-mgmt-${var.env}.azure-api.net/${local.api_base_path}"
}

output "test_storage_container_name" {
  value = "bulkscan"
}
