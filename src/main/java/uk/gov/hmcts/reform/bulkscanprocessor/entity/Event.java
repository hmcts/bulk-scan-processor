package uk.gov.hmcts.reform.bulkscanprocessor.entity;

// TODO: separate envelope state and events.
public enum Event {

    ENVELOPE_CREATED,
    DOC_FAILURE, // generic failure while processing zip file. before uploading to document management
    DOC_UPLOADED,
    DOC_UPLOAD_FAILURE,
    DOC_PROCESSED, // when blob is successfully deleted after storing all docs in DM
    DOC_CONSUMED, // client service handled the documents
}
