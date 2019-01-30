package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZipFileEnvelope {

    @JsonProperty("id")
    public final String id;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("status")
    public final String status;

    // region constructor
    public ZipFileEnvelope(String id, String container, String status) {
        this.id = id;
        this.container = container;
        this.status = status;
    }
    // endregion
}
