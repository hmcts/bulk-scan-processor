package uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessedEnvelope {

    public final UUID envelopeId;

    public final String ccdId;

    public final String envelopeCcdAction;

    public ProcessedEnvelope(
        @JsonProperty("envelope_id") UUID envelopeId,
        @JsonProperty("xx_ccd_id") String ccdId,
        @JsonProperty("envelope_ccd_action") String envelopeCcdAction
    ) {
        this.envelopeId = envelopeId;
        this.ccdId = ccdId;
        this.envelopeCcdAction = envelopeCcdAction;
    }
}