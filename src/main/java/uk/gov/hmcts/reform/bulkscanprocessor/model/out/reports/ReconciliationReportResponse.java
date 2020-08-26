package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ReconciliationReportResponse {

    @JsonProperty("discrepancies")
    public final List<DiscrepancyItem> items;

    public ReconciliationReportResponse(List<DiscrepancyItem> items) {
        this.items = items;
    }
}
