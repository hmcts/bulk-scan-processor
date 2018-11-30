package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

// TODO: separate envelope state and events.
public enum Event {

    DOC_FAILURE, // generic failure while processing zip file. before uploading to document management
    FILE_VALIDATION_FAILURE,
    DOC_SIGNATURE_FAILURE, // Signature verification failure while processing zip file
    DOC_UPLOADED,
    DOC_UPLOAD_FAILURE,
    DOC_PROCESSED, // when blob is successfully processed after storing all docs in DM
    DOC_CONSUMED, // client service handled the documents
    BLOB_DELETE_FAILURE, // when blob is not successfully deleted after processing is complete
    DOC_PROCESSED_NOTIFICATION_SENT, // when document processed notification is posted (to servicebus queue)
    DOC_PROCESSED_NOTIFICATION_FAILURE, // when document processed notification fails
}
