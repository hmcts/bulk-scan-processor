package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import javax.validation.ClockProvider;

/**
 * Represents a list of discrepancy items in the discrepancy report.
 */
public class EnvelopeCountSummaryReportListResponse {

    @JsonProperty("total_received")
    public final int totalReceived;

    @JsonProperty("total_rejected")
    public final int totalRejected;

    @JsonProperty("time_stamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public final LocalDateTime timeStamp;

    @JsonProperty("data")
    public final List<EnvelopeCountSummaryReportItem> items;

    /**
     * Constructor for EnvelopeCountSummaryReportListResponse.
     * @param items list of discrepancy items
     * @param clockProvider clock provider
     */
    public EnvelopeCountSummaryReportListResponse(
        List<EnvelopeCountSummaryReportItem> items,
        ClockProvider clockProvider
    ) {
        this.items = items;
        int totalReceivedEnvelopes = 0;
        int totalRejectedEnvelopes = 0;

        for (var item : items) {
            totalReceivedEnvelopes += item.received;
            totalRejectedEnvelopes += item.rejected;
        }

        this.totalReceived = totalReceivedEnvelopes;
        this.totalRejected = totalRejectedEnvelopes;
        timeStamp = LocalDateTime.now(clockProvider.getClock()).withNano(0);
    }
}
