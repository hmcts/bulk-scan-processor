package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnvelopeCountSummaryReportResponse {

    @JsonProperty("received")
    public final int received;

    @JsonProperty("rejected")
    public final int rejected;

    // region constructor
    public EnvelopeCountSummaryReportResponse(int received, int rejected) {
        this.received = received;
        this.rejected = rejected;
    }
    // endregion
}
