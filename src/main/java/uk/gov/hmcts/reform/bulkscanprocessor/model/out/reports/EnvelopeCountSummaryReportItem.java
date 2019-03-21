package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class EnvelopeCountSummaryReportItem {

    @JsonProperty("received")
    public final int received;

    @JsonProperty("rejected")
    public final int rejected;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("date")
    public final LocalDate date;

    // region constructor
    public EnvelopeCountSummaryReportItem(int received, int rejected, String container, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.container = container;
        this.date = date;
    }
    // endregion
}
