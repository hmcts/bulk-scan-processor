data "template_file" "metricalerttemplate" {
  template = "${file("${path.module}/templates/ServiceBusAlert.json")}"
}

resource "azurerm_template_deployment" "custom_alert" {
  template_body       = "${data.template_file.metricalerttemplate.rendered}"
  name                = "NAME?"
  resourceGroupName = "${var.resource_group_name}"
  deployment_mode     = "Incremental"

  parameters = {
  }
}
