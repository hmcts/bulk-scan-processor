package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("total_received")
    private final int totalReceived;

    @JsonProperty("total_rejected")
    private final int totalRejected;

    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public final LocalDateTime timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(
        List<EnvelopeCountSummaryReportItem> items
    ) {
        this.items = items;
        int totalReceivedEnvelops = 0;
        int totalRejectedEnvelops = 0;

        for (var item : items) {
            totalReceivedEnvelops += item.received;
            totalRejectedEnvelops += item.rejected;
        }

        this.totalReceived = totalReceivedEnvelops;
        this.totalRejected = totalRejectedEnvelops;
        timeStamp = LocalDateTime.now().withNano(0);
    }

    public int getTotalReceived() {
        return totalReceived;
    }

    public int getTotalRejected() {

        return totalRejected;
    }
}
