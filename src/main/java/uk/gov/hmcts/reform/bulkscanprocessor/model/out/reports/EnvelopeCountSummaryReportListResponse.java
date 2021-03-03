package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("total_received")
    public final int totalReceived;

    @JsonProperty("total_rejected")
    public final int totalRejected;

    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public final String timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(
        int totalReceived,
        int totalRejected,
        String localDateTime,
        List<EnvelopeCountSummaryReportItem> items
    ) {
        this.totalReceived = totalReceived;
        this.totalRejected = totalRejected;
        this.timeStamp = localDateTime;
        this.items = items;
    }
}
