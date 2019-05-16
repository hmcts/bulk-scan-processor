package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;

public class ZipFileEvent {

    @JsonProperty("type")
    public final String eventType;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    public final Instant createdAt;

    @JsonProperty("reason")
    public final String reason;

    // region constructor
    public ZipFileEvent(String eventType, String container, Instant createdAt, String reason) {
        this.eventType = eventType;
        this.container = container;
        this.createdAt = createdAt;
        this.reason = reason;
    }
    // endregion
}
