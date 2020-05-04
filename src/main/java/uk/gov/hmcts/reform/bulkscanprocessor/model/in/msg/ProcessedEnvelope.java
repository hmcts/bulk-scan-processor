package uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessedEnvelope {

    public final UUID id;

    public ProcessedEnvelope(@JsonProperty("id") UUID id) {
        this.id = id;
    }
}
