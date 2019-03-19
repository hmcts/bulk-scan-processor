package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import java.time.LocalDate;

public class EnvelopeCountSummary {

    public final int received;
    public final int rejected;
    public final String container;
    public final LocalDate date;

    // region constructor
    public EnvelopeCountSummary(int received, int rejected, String container, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.container = container;
        this.date = date;
    }
    // endregion
}
