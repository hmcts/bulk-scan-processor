package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;
import java.util.UUID;

public class EnvelopeInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("envelope_id")
    public final UUID envelopeId;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("created_at")
    public final Instant createdAt;

    public EnvelopeInfo(
        String container,
        String fileName,
        UUID envelopeId,
        Instant createdAt
    ) {
        this.container = container;
        this.fileName = fileName;
        this.envelopeId = envelopeId;
        this.createdAt = createdAt;
    }
}
