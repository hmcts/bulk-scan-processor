package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedEnvelope;

import java.util.List;

public class RejectedEnvelopesResponse {

    @JsonProperty("count")
    public final int count;

    @JsonProperty("rejected_envelopes")
    public final List<RejectedEnvelope> rejectedEnvelopes;

    public RejectedEnvelopesResponse(int count, List<RejectedEnvelope> rejectedEnvelopes) {
        this.count = count;
        this.rejectedEnvelopes = rejectedEnvelopes;
    }
}
