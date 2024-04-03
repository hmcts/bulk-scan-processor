package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;

/**
 * Represents an event that happened to a zip file.
 */
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

    /**
     * Constructor for the ZipFileEvent.
     * @param eventType type of the event
     * @param container container that the event is related to
     * @param createdAt time when the event happened
     * @param reason reason for the event
     */
    public ZipFileEvent(String eventType, String container, Instant createdAt, String reason) {
        this.eventType = eventType;
        this.container = container;
        this.createdAt = createdAt;
        this.reason = reason;
    }
}
