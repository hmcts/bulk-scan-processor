package uk.gov.hmcts.reform.bulkscanprocessor.entity;

public enum EnvelopeStatus {

    DOC_FAILURE, // generic failure while processing zip file. before uploading to document management
    DOC_UPLOADED,
    DOC_UPLOAD_FAILURE
}
