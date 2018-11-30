package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.DbScannableItem;

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
    private Document(
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

    public static Document fromScannableItem(DbScannableItem item) {
        return new Document(
            item.getFileName(),
            item.getDocumentControlNumber(),
            item.getDocumentType().toString(),
            item.getScanningDate().toInstant(),
            item.getDocumentUrl()
        );
    }
}
