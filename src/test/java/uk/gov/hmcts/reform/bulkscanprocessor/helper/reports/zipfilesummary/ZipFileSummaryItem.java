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
    private final String classification;
    private final String ccdId;
    private final String ccdAction;
    private final String envelopeId;

    public ZipFileSummaryItem(
        String zipFileName,
        Instant createdDate,
        Instant completedDate,
        String container,
        String lastEventStatus,
        String envelopeStatus,
        String classification,
        String ccdId,
        String ccdAction,
        String envelopeId
    ) {
        this.zipFileName = zipFileName;
        this.createdDate = createdDate;
        this.completedDate = completedDate;
        this.container = container;
        this.lastEventStatus = lastEventStatus;
        this.envelopeStatus = envelopeStatus;
        this.classification = classification;
        this.ccdId = ccdId;
        this.ccdAction = ccdAction;
        this.envelopeId = envelopeId;
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

    @Override
    public String getClassification() {
        return classification;
    }

    @Override
    public String getCcdId() {
        return ccdId;
    }

    @Override
    public String getCcdAction() {
        return ccdAction;
    }

    @Override
    public String getEnvelopeId() {
        return envelopeId;
    }
}
