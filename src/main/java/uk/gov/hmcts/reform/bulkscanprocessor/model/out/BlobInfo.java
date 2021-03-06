package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;


public class BlobInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("envelope_id")
    public final UUID envelopeId;

    @JsonProperty("created_at")
    public final String createdAt;

    public BlobInfo(
        String container,
        String fileName,
        UUID envelopeId,
        String createdAt
    ) {
        this.container = container;
        this.fileName = fileName;
        this.envelopeId = envelopeId;
        this.createdAt = createdAt;
    }
}
