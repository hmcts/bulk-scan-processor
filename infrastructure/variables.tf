variable "product" {
  type    = "string"
  default = "rpe"
}

variable "component" {
  type = "string"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "location_db" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "subscription" {
  type = "string"
}

variable "ilbIp"{}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type                        = "string"
  description                 = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "capacity" {
  default = "1"
}

variable "deployment_namespace" {
  default = ""
}

variable "account_name" {
  default = ""
}

variable "token_validity" {
  default = "86400" #1 day in seconds
}

variable "vault_section" {
  default = "test"
}

variable "s2s_name" {
  default = "bulk_scan_processor"
}

variable "common_tags" {
  type = "map"
}

variable "scheduling_enabled" {
  default = "false"
}

variable "scan_delay" {
  default = "30000"  # In milliseconds
}

# thumbprint of the SSL certificate for API gateway tests
variable api_gateway_test_certificate_thumbprint {
  type = "string"
  # keeping this empty by default, so that no thumbprint will match
  default = ""
}
