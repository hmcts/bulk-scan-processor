package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class EnvelopeCountSummaryReportResponse {

    @JsonProperty("received")
    public final int received;

    @JsonProperty("rejected")
    public final int rejected;

    @JsonProperty("jurisdiction")
    public final String jurisdiction;

    @JsonProperty("date")
    public final LocalDate date;

    // region constructor
    public EnvelopeCountSummaryReportResponse(int received, int rejected, String jurisdiction, LocalDate date) {
        this.received = received;
        this.rejected = rejected;
        this.jurisdiction = jurisdiction;
        this.date = date;
    }
    // endregion
}
