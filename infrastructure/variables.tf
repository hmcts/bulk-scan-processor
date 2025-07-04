variable "product" {}

variable "raw_product" {
  default = "bulk-scan" // jenkins-library overrides product for PRs and adds e.g. pr-118-bulk-scan
}

variable "component" {}

variable "location" {
  type    = string
  default = "UK South"
}

variable "location_db" {
  type    = string
  default = "UK South"
}

variable "env" {
  type = string
}

variable "subscription" {
  type = string
}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type        = string
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
  type = map(any)
}

variable "scan_delay" {
  default = "30000" # In milliseconds
}

variable "scan_enabled" {
  default = "false"
}

variable "storage_proxy_enabled" {
  default = "false"
}

variable "orchestrator_notifications_task_enabled" {
  default = "false"
}

variable "orchestrator_notifications_task_delay" {
  default = "30000" # in ms
}

# region delete rejected files
variable "delete_rejected_files_enabled" {
  default = "false"
}

variable "delete_rejected_files_cron" {
  default = "0 0 7 * * *"
}

variable "delete_rejected_files_ttl" {
  default = "PT72H"
}

# endregion

# region reports
variable "smtp_host" {
  type        = string
  default     = "false"
  description = "SMTP host for sending out reports via JavaMailSender"
}

variable "reports_cron" {
  default     = "0 0 18 ? * MON-FRI"
  description = "Cron signature for job to send out reports to be executed. Default value is 6PM (server time) every workday"
}

# endregion

# region incomplete envelopes monitoring

variable "incomplete_envelopes_cron" {
  default     = "0 */15 * * * *"
  description = "Cron signature for job to log amount of incomplete envelopes currently present in service"
}

variable "incomplete_envelopes_enabled" {
  default     = "false"
  description = "Task is trivial and no need to run. Will be enabled in production only though"
}

# endregion

variable "ocr_validation_url_probate" {
  default = ""
}

# The value of the parameter 'blob_lease_timeout' should be between 15 and 60 seconds.
variable "blob_lease_timeout" {
  default = "15"
}

variable "blob_selected_container" {
  default = "ALL"
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default     = ""
}

variable "process_payments_enabled" {
  default = "true"
}

variable "enable_ase" {
  default = false
}

variable "database_name" {
  default = "bulk_scan"
}

variable "postgresql_user" {
  default = "bulk_scanner"
}

variable "aks_subscription_id" {}
