package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.Optional;

public enum Status {

    CREATED,
    METADATA_FAILURE,   // when we are aware of envelope, but there are inconsistency among files and metadata info
    UPLOADED,
    UPLOAD_FAILURE,
    NOTIFICATION_SENT,  // after notifying about a new envelope
    ABORTED,            // envelope in inconsistent state has been aborted
    COMPLETED;          // final state - the envelope has been successfully processed by the service

    public static Optional<Status> fromEvent(Event event) {
        switch (event) {
            case DOC_FAILURE:
                return Optional.of(METADATA_FAILURE);
            case DOC_UPLOADED:
                return Optional.of(UPLOADED);
            case DOC_UPLOAD_FAILURE:
                return Optional.of(UPLOAD_FAILURE);
            case DOC_PROCESSED_NOTIFICATION_SENT:
                return Optional.of(NOTIFICATION_SENT);
            default:
                return Optional.empty();
        }
    }
}
