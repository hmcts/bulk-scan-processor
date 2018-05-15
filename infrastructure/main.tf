provider "azurerm" {}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

locals {
  app = "bulk-scanning"
}

module "backend" {
  source       = "git@github.com:hmcts/moj-module-webapp?ref=master"
  product      = "${var.product}-${local.app}"
  location     = "${var.location}"
  env          = "${var.env}"
  ilbIp        = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend  = false
  capacity     = "${var.capacity}"

  asp_name = "bulk-scanning"

  app_settings = {}
}

module "key-vault" {
  source                  = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  product                 = "${var.product}"
  env                     = "${var.env}"
  tenant_id               = "${var.tenant_id}"
  object_id               = "${var.jenkins_AAD_objectId}"
  resource_group_name     = "${module.backend.resource_group_name}"
  product_group_object_id = "300e771f-856c-45cc-b899-40d78281e9c1"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"
}

resource "azurerm_storage_account" "provider" {
  name                     = "bulkscanning"
  resource_group_name      = "${azurerm_resource_group.rg.name}"
  location                 = "${var.location}"
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_storage_container" "scans" {
  name                  = "scans"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
  storage_account_name  = "${azurerm_storage_account.provider.name}"
  container_access_type = "private"
}
