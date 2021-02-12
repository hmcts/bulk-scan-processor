package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.Optional;

public enum Status {

    CONSUMED,           // client service handled the documents
    CREATED,
    METADATA_FAILURE,   // when we are aware of envelope, but there are inconsistency among files and metadata info
    SIGNATURE_FAILURE,  // the zip archive failed signature verification
    ZIP_PROCESSING_FAILURE,  // the zip archive failed signature verification
    UPLOADED,
    UPLOAD_FAILURE,
    NOTIFICATION_SENT,  // after notifying about a new envelope
    COMPLETED;          // final state - the envelope has been successfully processed by the service

    public static Optional<Status> fromEvent(Event event) {
        switch (event) {
            case DOC_FAILURE:
                return Optional.of(METADATA_FAILURE);
            case DOC_SIGNATURE_FAILURE:
                return Optional.of(SIGNATURE_FAILURE);
            case DOC_UPLOADED:
                return Optional.of(UPLOADED);
            case DOC_UPLOAD_FAILURE:
                return Optional.of(UPLOAD_FAILURE);
            case DOC_CONSUMED:
                return Optional.of(CONSUMED);
            case DOC_PROCESSED_NOTIFICATION_SENT:
                return Optional.of(NOTIFICATION_SENT);
            default:
                return Optional.empty();
        }
    }

}
