package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZipFileEnvelope {

    @JsonProperty("id")
    public final String id;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("status")
    public final String status;

    @JsonProperty("ccd_id")
    public final String ccdId;

    @JsonProperty("envelope_ccd_action")
    public final String envelopeCcdAction;

    // region constructor
    public ZipFileEnvelope(String id, String container, String status, String ccdId, String envelopeCcdAction) {
        this.id = id;
        this.container = container;
        this.status = status;
        this.ccdId = ccdId;
        this.envelopeCcdAction = envelopeCcdAction;
    }
    // endregion
}
