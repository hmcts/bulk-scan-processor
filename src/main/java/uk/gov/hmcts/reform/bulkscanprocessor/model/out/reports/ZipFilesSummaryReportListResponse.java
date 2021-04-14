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

    public ZipFilesSummaryReportListResponse(List<ZipFilesSummaryReportItem> items) {
        this.total = items.size();
        this.totalCompleted = (int)items.stream().filter(
            completed -> "COMPLETED".equalsIgnoreCase(completed.envelopeStatus)).count();
        this.totalFailed = (int)items.stream().filter(
            completed -> completed.envelopeStatus != null && completed.envelopeStatus.contains("FAILURE")).count();
        this.items = items;
    }
}
