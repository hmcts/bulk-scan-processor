package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class ZipFileSummaryResponse {

    public final String fileName;
    public final LocalDate dateReceived;
    public final LocalTime timeReceived;
    public final LocalDate dateProcessed;
    public final LocalTime timeProcessed;
    public final String container;
    public final String lastEventStatus;
    public final String envelopeStatus;
    public final String classification;

    // region constructor
    public ZipFileSummaryResponse(
        String fileName,
        LocalDate dateReceived,
        LocalTime timeReceived,
        LocalDate dateProcessed,
        LocalTime timeProcessed,
        String container,
        String lastEventStatus,
        String envelopeStatus,
        String classification
    ) {
        this.fileName = fileName;
        this.dateReceived = dateReceived;
        this.timeReceived = timeReceived;
        this.dateProcessed = dateProcessed;
        this.timeProcessed = timeProcessed;
        this.container = container;
        this.lastEventStatus = lastEventStatus;
        this.envelopeStatus = envelopeStatus;
        this.classification = classification;
    }
    // endregion
}
