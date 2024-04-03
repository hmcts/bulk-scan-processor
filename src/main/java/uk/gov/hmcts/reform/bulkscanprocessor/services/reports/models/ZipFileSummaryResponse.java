package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents the zip file summary response.
 */
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
    public final String ccdId;
    public final String ccdAction;

    /**
     * Constructor for the ZipFileSummaryResponse.
     * @param fileName The file name
     * @param dateReceived The date received
     * @param timeReceived The time received
     * @param dateProcessed The date processed
     * @param timeProcessed The time processed
     * @param container The container
     * @param lastEventStatus The last event status
     * @param envelopeStatus The envelope status
     * @param classification The classification
     * @param ccdId The ccd id
     * @param ccdAction The ccd action
     */
    public ZipFileSummaryResponse(
        String fileName,
        LocalDate dateReceived,
        LocalTime timeReceived,
        LocalDate dateProcessed,
        LocalTime timeProcessed,
        String container,
        String lastEventStatus,
        String envelopeStatus,
        String classification,
        String ccdId,
        String ccdAction
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
        this.ccdId = ccdId;
        this.ccdAction = ccdAction;
    }
}
