package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

public class EnvelopeCountSummary {

    public final int received;
    public final int rejected;

    // region constructor
    public EnvelopeCountSummary(int received, int rejected) {
        this.received = received;
        this.rejected = rejected;
    }
    // endregion
}
