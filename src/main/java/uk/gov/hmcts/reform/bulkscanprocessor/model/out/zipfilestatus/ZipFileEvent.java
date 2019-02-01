package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class ZipFileEvent {

    @JsonProperty("type")
    public final String eventType;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("created_at")
    public final Instant createdAt;

    // region constructor
    public ZipFileEvent(String eventType, String container, Instant createdAt) {
        this.eventType = eventType;
        this.container = container;
        this.createdAt = createdAt;
    }
    // endregion
}
