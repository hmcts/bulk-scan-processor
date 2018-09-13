package uk.gov.hmcts.reform.bulkscanprocessor.entity;

// TODO: separate envelope state and events.
public enum Event {

    DOC_FAILURE, // generic failure while processing zip file. before uploading to document management
    DOC_UPLOADED,
    DOC_UPLOAD_FAILURE,
    DOC_PROCESSED, // when blob is successfully processed after storing all docs in DM
    DOC_CONSUMED, // client service handled the documents
    BLOB_DELETE_FAILURE, // when blob is not successfully deleted after processing is complete
    DOC_PROCESSED_NOTIFICATION_SENT, // when document processed notification is posted (to servicebus queue)
}
