package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Document {

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("control_number")
    public final String controlNumber;

    @JsonProperty("type")
    public final String type;

    @JsonProperty("scanned_at")
    public final Instant scannedAt;

    @JsonProperty("url")
    public final String url;

    // region constructor
    public Document(
        String fileName,
        String controlNumber,
        String type,
        Instant scannedAt,
        String url
    ) {
        this.fileName = fileName;
        this.controlNumber = controlNumber;
        this.type = type;
        this.scannedAt = scannedAt;
        this.url = url;
    }
    // endregion
}
