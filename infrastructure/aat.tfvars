vault_section = "preprod"
capacity = "2"
reupload_delay = "4000"
reupload_enabled = "true"
scan_delay = "4000"
scan_enabled = "true"

orchestrator_notifications_task_enabled = "true"
orchestrator_notifications_task_delay = "3000"

delete_rejected_files_enabled = "true"
delete_rejected_files_cron = "0 0/10 * * * *"

api_gateway_test_valid_certificate_thumbprint = "344F8D3870B5BAA8CBDCCA0B1993852FC147BF5D"
allowed_client_certificate_thumbprints = ["4AF638E82FBD9959A5B8286C673360AC2C2E7053"]
blob_signature_verification_key_file = "nonprod_public_key.der"

incomplete_envelopes_cron = "0 0 * * * *"

ocr_validation_url_bulkscan_sample_app = "http://bulk-scan-sample-app-aat.service.core-compute-aat.internal"
