package uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessedEnvelope {

    public final UUID id;

    public final UUID envelopeId;

    public final Long ccdId;

    public final String envelopeCcdAction;

    public ProcessedEnvelope(
        @JsonProperty("id") UUID id,
        @JsonProperty("envelope_id") UUID envelopeId,
        @JsonProperty("ccd_id") Long ccdId,
        @JsonProperty("envelope_ccd_action") String envelopeCcdAction
    ) {
        this.id = id;
        this.envelopeId = envelopeId;
        this.ccdId = ccdId;
        this.envelopeCcdAction = envelopeCcdAction;
    }
}