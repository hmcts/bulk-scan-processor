package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;

public class EnvelopeCountSummaryResponse {

    public final int totalReceived;
    public final int totalRejected;
    public final String timeStamp;
    public final List<EnvelopeCountSummary> items;

    public EnvelopeCountSummaryResponse(
        int totalReceived,
        int totalRejected,
        String localDateTime,
        List<EnvelopeCountSummary> items
    ) {
        this.totalReceived = totalReceived;
        this.totalRejected = totalRejected;
        this.timeStamp = localDateTime;
        this.items = items;
    }
}
