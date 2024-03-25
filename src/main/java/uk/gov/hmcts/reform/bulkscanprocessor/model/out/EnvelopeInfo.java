package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the information about an envelope.
 */
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

    /**
     * Constructor for EnvelopeInfo.
     * @param container name of the container
     * @param fileName name of the envelope
     * @param envelopeId ID of the envelope
     * @param createdAt time when the envelope was created
     */
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
