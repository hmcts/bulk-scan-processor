package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;
import java.util.UUID;

public interface RejectedZipFile {
    String getZipFileName();

    String getContainer();

    String getEvent();

    Instant getProcessingStartedEventDate();

    UUID getEnvelopeId();
}
