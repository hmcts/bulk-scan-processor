package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import java.time.Instant;

public class ZipFileSummary {

    public final String zipFileName;
    public final Instant createdDate;
    public final Instant completedDate;
    public final String jurisdiction;
    public final String status;

    // region constructor
    public ZipFileSummary(String zipFileName,
                          Instant createdDate,
                          Instant completedDate,
                          String jurisdiction,
                          String status
    ) {
        this.zipFileName = zipFileName;
        this.createdDate = createdDate;
        this.completedDate = completedDate;
        this.jurisdiction = jurisdiction;
        this.status = status;
    }
    // endregion
}
