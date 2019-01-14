package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(List<EnvelopeCountSummaryReportItem> items) {
        this.items = items;
    }
}
