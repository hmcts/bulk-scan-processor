variable "product" {}

variable "raw_product" {
  default = "bulk-scan" // jenkins-library overrides product for PRs and adds e.g. pr-118-bulk-scan
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

variable "ilbIp" {}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type        = "string"
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
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

variable "test_s2s_name" {
  default = "bulk_scan_processor_tests"
}

variable "common_tags" {
  type = "map"
}

variable "reupload_batch" {
  default = "20"
}

variable "reupload_delay" {
  default = "1800000" # In milliseconds
}

variable "reupload_max_tries" {
  default = "5"
}

variable "reupload_enabled" {
  default = "false"
}

variable "scan_delay" {
  default = "30000" # In milliseconds
}

variable "scan_enabled" {
  default = "false"
}

# list of SSL client certificate thumbprints that are accepted by the API (gateway)
# (excludes certificates used by API tests)
variable "allowed_client_certificate_thumbprints" {
  type    = "list"
  default = []
}

# The value of the parameter 'blob_lease_timeout' should be between 15 and 60 seconds.
variable "blob_lease_timeout" {
  default = "15"
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default     = ""
}
