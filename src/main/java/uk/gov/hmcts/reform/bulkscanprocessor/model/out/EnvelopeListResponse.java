package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EnvelopeListResponse {

    @JsonProperty("envelopes")
    public final List<EnvelopeResponse> envelopes;

    @JsonCreator
    public EnvelopeListResponse(@JsonProperty("envelopes") List<EnvelopeResponse> envelopes) {
        this.envelopes = envelopes;
    }
}
