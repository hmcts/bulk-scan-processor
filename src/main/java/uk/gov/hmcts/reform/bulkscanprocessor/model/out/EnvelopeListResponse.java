package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the response containing a list of envelopes.
 */
public class EnvelopeListResponse {

    @JsonProperty("envelopes")
    public final List<EnvelopeResponse> envelopes;

    /**
     * Constructor for EnvelopeListResponse.
     * @param envelopes list of envelopes
     */
    @JsonCreator
    public EnvelopeListResponse(@JsonProperty("envelopes") List<EnvelopeResponse> envelopes) {
        this.envelopes = envelopes;
    }
}
