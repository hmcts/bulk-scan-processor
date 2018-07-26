output "app_namespace" {
  value = "${var.deployment_namespace}"
}

output "vaultName" {
  value = "${local.vaultName}"
}

output "microserviceName" {
  value = "${var.component}"
}
