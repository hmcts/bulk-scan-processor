package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.util.Optional;

public enum Status {

    CONSUMED, // client service handled the documents
    CREATED,
    PROCESSED, // when blob is successfully deleted after storing all docs in DM
    UPLOADED,
    UPLOAD_FAILURE;

    public static Optional<Status> fromEvent(Event event) {
        Status status = null;

        switch (event) {
            case DOC_UPLOADED:
                status = UPLOADED;

                break;
            case DOC_UPLOAD_FAILURE:
                status = UPLOAD_FAILURE;

                break;
            case DOC_PROCESSED:
                status = PROCESSED;

                break;
            case DOC_CONSUMED:
                status = CONSUMED;

                break;
            default:
                break;
        }

        return Optional.ofNullable(status);
    }
}
