data "template_file" "metricalerttemplate" {
  template = "${file("${path.cwd}/service-bus-alert/template/ServiceBusAlert.json")}"
}

resource "azurerm_template_deployment" "custom_alert" {
  template_body       = "${data.template_file.metricalerttemplate.rendered}"
  name                = "NAME?"
  resourceGroup = "${var.resource_group_name}"
  deployment_mode     = "Incremental"
  subscriptionId = "${var.subscription_id}"
  namespace = "${var.namespace}"

  parameters = {
  }
}
