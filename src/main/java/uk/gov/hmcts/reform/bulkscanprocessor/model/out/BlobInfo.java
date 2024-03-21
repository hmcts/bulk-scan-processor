package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;

/**
 * Represents the information about a blob.
 */
public class BlobInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("created_at")
    public final Instant createdAt;

    /**
     * Constructor for BlobInfo.
     * @param container name of the container
     * @param fileName name of the blob
     * @param createdAt time when the blob was created
     */
    public BlobInfo(
        String container,
        String fileName,
        Instant createdAt
    ) {
        this.container = container;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }
}
