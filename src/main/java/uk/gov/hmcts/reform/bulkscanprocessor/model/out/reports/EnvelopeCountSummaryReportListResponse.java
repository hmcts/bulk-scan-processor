package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
//import java.time.LocalDateTime;
import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("total_received")
    public final int totalReceived;

    @JsonProperty("total_rejected")
    public final int totalRejected;

    // "time_stamp" : "18-07-2017 06:20:19",
    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public final Timestamp timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(
        int totalReceived,
        int totalRejected,
        Timestamp localDateTime,
        List<EnvelopeCountSummaryReportItem> items
    ) {
        this.totalReceived = totalReceived;
        this.totalRejected = totalRejected;
        this.timeStamp = localDateTime;
        this.items = items;
    }
}
