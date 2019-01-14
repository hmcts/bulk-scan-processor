package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportResponse> items;

    public EnvelopeCountSummaryReportListResponse(List<EnvelopeCountSummaryReportResponse> items) {
        this.items = items;
    }
}
