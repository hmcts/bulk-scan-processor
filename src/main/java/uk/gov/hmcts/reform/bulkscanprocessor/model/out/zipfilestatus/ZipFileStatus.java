package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ZipFileStatus {

    @JsonProperty("envelopes")
    public final List<ZipFileEnvelope> envelopes;

    @JsonProperty("events")
    public final List<ZipFileEvent> events;

    // region constructor
    public ZipFileStatus(List<ZipFileEnvelope> envelopes, List<ZipFileEvent> events) {
        this.envelopes = envelopes;
        this.events = events;
    }
    // endregion
}
