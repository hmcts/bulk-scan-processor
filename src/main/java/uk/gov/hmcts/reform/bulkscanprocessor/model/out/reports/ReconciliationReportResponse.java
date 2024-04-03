package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response for reconciliation report.
 */
public class ReconciliationReportResponse {

    @JsonProperty("discrepancies")
    public final List<DiscrepancyItem> items;

    /**
     * Constructor for ReconciliationReportResponse.
     * @param items list of discrepancies
     */
    public ReconciliationReportResponse(List<DiscrepancyItem> items) {
        this.items = items;
    }
}
