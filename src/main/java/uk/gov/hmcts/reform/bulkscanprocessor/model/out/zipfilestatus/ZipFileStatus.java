package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ZipFileStatus {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("file_name")
    public final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("ccd_id")
    public final String ccdId;

    @JsonProperty("envelopes")
    public final List<ZipFileEnvelope> envelopes;

    @JsonProperty("events")
    public final List<ZipFileEvent> events;

    // region constructor
    public ZipFileStatus(String fileName, String ccdId, List<ZipFileEnvelope> envelopes, List<ZipFileEvent> events) {
        this.fileName = fileName;
        this.ccdId = ccdId;
        this.envelopes = envelopes;
        this.events = events;
    }
    // endregion
}
