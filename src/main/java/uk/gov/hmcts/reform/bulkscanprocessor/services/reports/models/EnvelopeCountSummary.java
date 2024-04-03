package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.time.LocalDate;

/**
 * Represents a summary of envelope counts.
 */
public class EnvelopeCountSummary {

    public final int received;
    public final int rejected;
    public final String container;
    public final LocalDate date;

    /**
     * Constructor for the EnvelopeCountSummary.
     * @param received The number of received envelopes
     * @param rejected The number of rejected envelopes
     * @param container The container
     * @param date The date
     */
    public EnvelopeCountSummary(int received, int rejected, String container, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.container = container;
        this.date = date;
    }
}
