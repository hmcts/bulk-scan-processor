package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ZipFilesSummaryReportListResponse {

    @JsonProperty("data")
    public final List<ZipFilesSummaryReportItem> items;

    public ZipFilesSummaryReportListResponse(List<ZipFilesSummaryReportItem> items) {
        this.items = items;
    }
}
