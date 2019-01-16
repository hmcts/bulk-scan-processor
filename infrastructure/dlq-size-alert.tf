module "dlq-size-alert" {
  source            = "./service-bus-alert"
  subscription_id = "${var.subscription}"
  resource_group_name = "${var.product}-${var.component}-${var.env}"
  namespace = "${var.deployment_namespace}"
}
