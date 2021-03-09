package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;

public class BlobInfo {

    @JsonProperty("container")
    public final String container;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("created_at")
    public final Instant createdAt;

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
