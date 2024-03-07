output "app_namespace" {
  value = var.deployment_namespace
}

output "TEST_SCAN_DELAY" {
  value = var.scan_delay
}

output "TEST_STORAGE_ACCOUNT_NAME" {
  value     = local.storage_account_name
  sensitive = true
}

output "TEST_S2S_URL" {
  value = local.s2s_url
}

output "TEST_S2S_NAME" {
  sensitive = true
  value     = var.test_s2s_name
}

output "TEST_STORAGE_ACCOUNT_URL" {
  value = local.storage_account_url
}

output "test_storage_container_name" {
  value = "bulkscan"
}
