package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

// TODO: separate envelope state and events.
/**
 * Events that can happen to an envelope.
 */
public enum Event {

    ZIPFILE_PROCESSING_STARTED, // when processor starts processing zipfile from blob
    DOC_FAILURE, // generic failure while processing zip file. before uploading to document management
    FILE_VALIDATION_FAILURE,
    DISABLED_SERVICE_FAILURE,
    FILE_SIZE_EXCEED_UPLOAD_LIMIT_FAILURE,
    DOC_UPLOADED,
    DOC_UPLOAD_FAILURE,
    DOC_PROCESSED_NOTIFICATION_SENT, // when document processed notification is posted (to servicebus queue)
    DOC_PROCESSED_NOTIFICATION_FAILURE, // when document processed notification fails
    DOC_PROCESSING_ABORTED, // when envelope processing cannot be completed (used manually to set the event with reason)
    COMPLETED, // the processing of the envelope completed successfully
    // when envelope status needs to be updated for reprocessing (used manually to set the event with reason)
    MANUAL_STATUS_CHANGE,
    MANUAL_RETRIGGER_PROCESSING
}
