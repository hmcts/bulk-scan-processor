package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import java.time.LocalDate;

public class EnvelopeCountSummary {

    public final int received;
    public final int rejected;
    public final String jurisdiction;
    public final LocalDate date;

    // region constructor
    public EnvelopeCountSummary(int received, int rejected, String jurisdiction, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.jurisdiction = jurisdiction;
        this.date = date;
    }
    // endregion
}
