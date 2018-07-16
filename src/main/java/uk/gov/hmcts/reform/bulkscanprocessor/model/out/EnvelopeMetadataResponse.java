package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.util.List;

public class EnvelopeMetadataResponse {

    @JsonProperty("envelopes")
    public final List<Envelope> envelopes;

    public EnvelopeMetadataResponse(List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
