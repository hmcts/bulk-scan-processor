terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.110.0"
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
