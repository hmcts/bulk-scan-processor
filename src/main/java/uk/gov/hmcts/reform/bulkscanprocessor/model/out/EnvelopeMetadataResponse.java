package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EnvelopeMetadataResponse {

    @JsonProperty("envelopes")
    public final List<EnvelopeResponse> envelopes;

    @JsonCreator
    public EnvelopeMetadataResponse(@JsonProperty("envelopes") List<EnvelopeResponse> envelopes) {
        this.envelopes = envelopes;
    }
}
