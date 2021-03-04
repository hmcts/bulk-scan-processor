package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("total_received")
    private int totalReceived = 0;

    @JsonProperty("total_rejected")
    private int totalRejected = 0;

    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public final LocalDateTime timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(
        List<EnvelopeCountSummaryReportItem> items
    ) {
        this.items = items;
        calculateTotalCounts(items);
        timeStamp = LocalDateTime.now().withNano(0);
    }

    public int getTotalReceived() {
        return totalReceived;
    }

    public int getTotalRejected() {
        return totalRejected;
    }

    private void calculateTotalCounts(List<EnvelopeCountSummaryReportItem> items) {
        for (var item : items) {
            this.totalReceived += item.received;
            this.totalRejected += item.rejected;
        }
    }
}
