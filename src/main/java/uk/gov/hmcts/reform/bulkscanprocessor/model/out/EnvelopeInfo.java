package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class EnvelopeInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("envelope_id")
    public final String envelopeId;

    @JsonProperty("created_at")
    public final String createdAt;

    public EnvelopeInfo(
        String container,
        String fileName,
        UUID envelopeId,
        String createdAt
    ) {
        this.container = container;
        this.fileName = fileName;
        if (envelopeId == null) {
            this.envelopeId = null;
        } else {
            this.envelopeId = envelopeId.toString();
        }

        this.createdAt = createdAt;
    }
}
