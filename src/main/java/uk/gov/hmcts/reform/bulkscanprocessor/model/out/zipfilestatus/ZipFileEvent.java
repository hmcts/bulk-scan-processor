package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampSerialiser;

import java.time.Instant;

public class ZipFileEvent {

    @JsonProperty("type")
    public final String eventType;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("created_at")
    @JsonSerialize(using = CustomTimestampSerialiser.class)
    public final Instant createdAt;

    // region constructor
    public ZipFileEvent(String eventType, String container, Instant createdAt) {
        this.eventType = eventType;
        this.container = container;
        this.createdAt = createdAt;
    }
    // endregion
}
