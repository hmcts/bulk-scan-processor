module "dlq-size-alert" {
  source            = "./service-bus-alert"
  subscription_id = "${var.subscription}"
  resourcegroup_name = "bulk-scan-aat" # "${var.product}-${var.env}"
  namespace = "bulkscan-dev" # "${var.deployment_namespace}"
  # found those ^ when manually created alert
}
