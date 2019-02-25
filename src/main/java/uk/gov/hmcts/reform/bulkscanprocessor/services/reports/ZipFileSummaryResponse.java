package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import java.time.LocalDate;
import java.time.LocalTime;

public class ZipFileSummaryResponse {

    public final String fileName;
    public final LocalDate dateReceived;
    public final LocalTime timeReceived;
    public final LocalDate dateProcessed;
    public final LocalTime timeProcessed;
    public final String jurisdiction;
    public final String status;

    // region constructor
    public ZipFileSummaryResponse(
        String fileName,
        LocalDate dateReceived,
        LocalTime timeReceived,
        LocalDate dateProcessed,
        LocalTime timeProcessed,
        String jurisdiction,
        String status
    ) {
        this.fileName = fileName;
        this.dateReceived = dateReceived;
        this.timeReceived = timeReceived;
        this.dateProcessed = dateProcessed;
        this.timeProcessed = timeProcessed;
        this.jurisdiction = jurisdiction;
        this.status = status;
    }
    // endregion
}
