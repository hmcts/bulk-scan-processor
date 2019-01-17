module "dlq-size-alert" {
  source            = "./service-bus-alert"
  subscription_id = "${var.subscription}"
  resourcegroup_name = "${var.product}-${var.env}"
  namespace = "${var.deployment_namespace}"
}
