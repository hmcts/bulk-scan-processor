package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ZipFilesSummaryReportListResponse {

    @JsonProperty("total")
    public final int total;

    @JsonProperty("total_completed")
    public final int totalCompleted;

    @JsonProperty("total_failed")
    public final int totalFailed;

    @JsonProperty("data")
    public final List<ZipFilesSummaryReportItem> items;

    public ZipFilesSummaryReportListResponse(int total, int totalCompleted,
                                             int totalFailed, List<ZipFilesSummaryReportItem> items) {
        this.total = total;
        this.totalCompleted = totalCompleted;
        this.totalFailed = totalFailed;
        this.items = items;
    }
}
