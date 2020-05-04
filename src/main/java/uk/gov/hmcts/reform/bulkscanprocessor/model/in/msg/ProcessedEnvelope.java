package uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessedEnvelope {

    public final UUID id;
    public final String processedCcdReference;
    public final String processedCcdType;

    public ProcessedEnvelope(
        @JsonProperty("id") UUID id,
        @JsonProperty("processed_ccd_reference") String processedCcdReference,
        @JsonProperty("processed_ccd_type") String processedCcdType
    ) {
        this.id = id;
        this.processedCcdReference = processedCcdReference;
        this.processedCcdType = processedCcdType;
    }
}