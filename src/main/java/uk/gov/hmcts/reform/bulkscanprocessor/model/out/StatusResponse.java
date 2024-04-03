package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

/**
 * Represents the status of the response.
 */
public enum StatusResponse {

    CONSUMED, // client service handled the documents
    CREATED,
    PROCESSED, // when blob is successfully deleted after storing all docs in DM
    UPLOADED,
    UPLOAD_FAILURE;

}
