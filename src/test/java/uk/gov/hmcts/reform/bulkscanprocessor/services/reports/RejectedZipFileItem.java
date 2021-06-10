package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;

import java.time.Instant;
import java.util.UUID;

public class RejectedZipFileItem implements RejectedZipFile {
    private final String zipFileName;
    private final String container;
    private final Instant processingStartedEventDate;
    private final UUID envelopeId;
    private final String event;

    public RejectedZipFileItem(
            String zipFileName,
            String container,
            Instant processingStartedEventDate,
            UUID envelopeId,
            String event
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.processingStartedEventDate = processingStartedEventDate;
        this.envelopeId = envelopeId;
        this.event = event;
    }

    @Override
    public String getZipFileName() {
        return zipFileName;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public Instant getProcessingStartedEventDate() {
        return processingStartedEventDate;
    }

    @Override
    public UUID getEnvelopeId() {
        return envelopeId;
    }

    @Override
    public String getEvent() {
        return event;
    }
}
