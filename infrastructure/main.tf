provider "azurerm" {}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

locals {
  app = "bulk-scanning"
  base_account_name = "${var.product}bulkscan${var.env}"
  account_name = "${replace(local.base_account_name, "-", "")}"
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

  app_settings = {
    STORAGE_ACCOUNT_NAME   = "${azurerm_storage_account.provider.name}"
    STORAGE_KEY            = "${azurerm_storage_account.provider.primary_access_key}"
  }
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
  name                      = "${substr(local.account_name, 0, min(length(local.account_name), 24))}"
  resource_group_name       = "${azurerm_resource_group.rg.name}"
  location                  = "${var.location}"
  account_tier              = "Standard"
  account_replication_type  = "LRS"
  account_kind              = "BlobStorage"
  enable_https_traffic_only = true
}

resource "azurerm_storage_container" "sscs" {
  name                  = "sscs"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
  storage_account_name  = "${azurerm_storage_account.provider.name}"
  container_access_type = "private"

  depends_on = ["azurerm_storage_account.provider"]
}
