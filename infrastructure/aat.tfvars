vault_section = "preprod"
capacity      = "2"
scan_delay    = "4000"
scan_enabled  = "true"

orchestrator_notifications_task_enabled = "true"
orchestrator_notifications_task_delay   = "3000"

delete_rejected_files_enabled = "true"
delete_rejected_files_cron    = "0 0/10 * * * *"

allowed_client_certificate_thumbprints = ["2A1EE53E044632145C89FE428128080353127DF6"]

incomplete_envelopes_cron = "0 0 * * * *"

ocr_validation_url_bulkscan_sample_app = "http://bulk-scan-sample-app-aat.service.core-compute-aat.internal"
