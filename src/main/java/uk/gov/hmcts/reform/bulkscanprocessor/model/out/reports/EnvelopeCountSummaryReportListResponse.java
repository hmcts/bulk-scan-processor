package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class EnvelopeCountSummaryReportListResponse {

    // "total_received"
    @JsonProperty("total_received")
    public final int totalReceived;

    // "total_rejected":
    @JsonProperty("total_rejected")
    public final int totalRejected;

    // "time_stamp" : "18-07-2017 06:20:19",
    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "dd-mm-yyyy HH:mm:ss")
    public final LocalDateTime timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    public EnvelopeCountSummaryReportListResponse(
        int totalReceived,
        int totalRejected,
        LocalDateTime localDateTime,
        List<EnvelopeCountSummaryReportItem> items
    ) {

        //initialize the fields
        this.totalReceived = totalReceived;
        this.totalRejected = totalRejected;
        this.timeStamp = localDateTime;

        this.items = items;

    }
}
