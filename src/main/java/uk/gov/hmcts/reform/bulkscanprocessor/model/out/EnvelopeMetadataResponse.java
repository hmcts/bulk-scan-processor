package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.util.List;

public class EnvelopeMetadataResponse {

    @JsonProperty("envelopes")
    public final List<Envelope> envelopes;

    @JsonCreator
    public EnvelopeMetadataResponse(List<Envelope> envelopes) {
        this.envelopes = envelopes;
    }
}
