package uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummary;

import java.time.Instant;

public class ZipFileSummaryItem implements ZipFileSummary {

    private final String zipFileName;
    private final Instant createdDate;
    private final Instant completedDate;
    private final String container;
    private final String lastEventStatus;
    private final String envelopeStatus;

    public ZipFileSummaryItem(
        String zipFileName,
        Instant createdDate,
        Instant completedDate,
        String container,
        String lastEventStatus,
        String envelopeStatus
    ) {
        this.zipFileName = zipFileName;
        this.createdDate = createdDate;
        this.completedDate = completedDate;
        this.container = container;
        this.lastEventStatus = lastEventStatus;
        this.envelopeStatus = envelopeStatus;
    }

    @Override
    public String getZipFileName() {
        return zipFileName;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public Instant getCompletedDate() {
        return completedDate;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public String getLastEventStatus() {
        return lastEventStatus;
    }

    @Override
    public String getEnvelopeStatus() {
        return envelopeStatus;
    }
}
