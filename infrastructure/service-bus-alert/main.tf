provider "azurerm" {
  version = "1.19.0"
}

data "template_file" "metricalerttemplate" {
  template = "${file("${path.cwd}/service-bus-alert/template/ServiceBusAlert.json")}"
}

resource "azurerm_template_deployment" "custom_alert" {
  template_body       = "${data.template_file.metricalerttemplate.rendered}"
  name                = "__NAME__"
  deployment_mode     = "Incremental"
  resource_group_name = "${var.resourcegroup_name}"

  parameters = {
    subscriptionId = "${var.subscription_id}"
    resourceGroup = "${var.resourcegroup_name}"
    namespace = "${var.namespace}"
  }
}
