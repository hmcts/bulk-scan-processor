package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Represents a discrepancy item in the discrepancy report.
 */
public class EnvelopeCountSummaryReportItem {

    @JsonProperty("received")
    public final int received;

    @JsonProperty("rejected")
    public final int rejected;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("date")
    public final LocalDate date;

    /**
     * Constructor for EnvelopeCountSummaryReportItem.
     * @param received number of received envelopes
     * @param rejected number of rejected envelopes
     * @param container container name
     * @param date date
     */
    public EnvelopeCountSummaryReportItem(int received, int rejected, String container, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.container = container;
        this.date = date;
    }
}
