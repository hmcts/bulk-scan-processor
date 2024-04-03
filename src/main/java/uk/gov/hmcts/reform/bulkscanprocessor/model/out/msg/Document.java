package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;

import java.time.Instant;

/**
 * Represents a document that is being sent to downstream systems.
 */
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    public final Instant scannedAt;

    @JsonProperty("uuid")
    public final String uuid;

    /**
     * Constructor for document.
     * @param fileName file name
     * @param controlNumber control number
     * @param type document type
     * @param subtype document subtype
     * @param scannedAt scanned at
     * @param uuid document uuid
     */
    private Document(
        String fileName,
        String controlNumber,
        DocumentType type,
        String subtype,
        Instant scannedAt,
        String uuid
    ) {
        this.fileName = fileName;
        this.controlNumber = controlNumber;
        this.type = type;
        this.subtype = subtype;
        this.scannedAt = scannedAt;
        this.uuid = uuid;
    }

    /**
     * Creates a Document object from ScannableItem.
     * @param item ScannableItem
     * @return Document
     */
    public static Document fromScannableItem(ScannableItem item) {
        return new Document(
            item.getFileName(),
            item.getDocumentControlNumber(),
            item.getDocumentType(),
            item.getDocumentSubtype(),
            item.getScanningDate(),
            item.getDocumentUuid()
        );
    }
}
