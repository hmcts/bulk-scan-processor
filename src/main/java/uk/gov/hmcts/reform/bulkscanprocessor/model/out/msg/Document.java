package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;

import java.time.Instant;

public class Document {

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("control_number")
    public final String controlNumber;

    @JsonProperty("type")
    public final DocumentType type;

    @JsonProperty("subtype")
    public final String subtype;

    @JsonProperty("scanned_at")
    public final Instant scannedAt;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("uuid")
    public final String uuid;

    // region constructor
    private Document(
        String fileName,
        String controlNumber,
        DocumentType type,
        String subtype,
        Instant scannedAt,
        String url,
        String uuid
    ) {
        this.fileName = fileName;
        this.controlNumber = controlNumber;
        this.type = type;
        this.subtype = subtype;
        this.scannedAt = scannedAt;
        this.url = url;
        this.uuid = uuid;
    }
    // endregion

    public static Document fromScannableItem(ScannableItem item) {
        return new Document(
            item.getFileName(),
            item.getDocumentControlNumber(),
            item.getDocumentType(),
            item.getDocumentSubtype(),
            item.getScanningDate(),
            item.getDocumentUrl(),
            item.getDocumentUuid()
        );
    }
}
