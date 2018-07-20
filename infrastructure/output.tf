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

# this variable will be accessible to tests as API_GATEWAY_URL environment variable
output "api_gateway_url" {
  value = "https://core-api-mgmt-${var.env}.azure-api.net/${local.api_base_path}"
}
