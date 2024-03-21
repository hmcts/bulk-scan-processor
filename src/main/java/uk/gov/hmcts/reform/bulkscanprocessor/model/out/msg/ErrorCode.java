package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

/**
 * Error codes that can be returned in the response.
 */
public enum ErrorCode {

    ERR_FILE_LIMIT_EXCEEDED, // size too big
    ERR_METAFILE_INVALID,
    ERR_PAYMENTS_DISABLED, //payments not allowed for the container or not allowed in specific environment
    ERR_SERVICE_DISABLED, // service is disabled in specific environment
    ERR_AV_FAILED, // antivirus scan failed
    ERR_SIG_VERIFY_FAILED, // signature does not match the zip content
    ERR_RESCAN_REQUIRED,
    ERR_ZIP_PROCESSING_FAILED, // invalid zip file content
}
