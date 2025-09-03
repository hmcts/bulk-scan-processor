terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.42.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = ">= 2.38.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
