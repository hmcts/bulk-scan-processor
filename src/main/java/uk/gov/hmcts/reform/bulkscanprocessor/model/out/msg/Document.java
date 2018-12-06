package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.time.Instant;
import java.util.Map;

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

    @JsonProperty("ocr_data")
    public final Map<String, String> ocrData;

    // region constructor
    private Document(
        String fileName,
        String controlNumber,
        String type,
        Instant scannedAt,
        String url,
        Map<String, String> ocrData
    ) {
        this.fileName = fileName;
        this.controlNumber = controlNumber;
        this.type = type;
        this.scannedAt = scannedAt;
        this.url = url;
        this.ocrData = ocrData;
    }
    // endregion

    public static Document fromScannableItem(ScannableItem item) {
        return new Document(
            item.getFileName(),
            item.getDocumentControlNumber(),
            item.getDocumentType().toString(),
            item.getScanningDate().toInstant(),
            item.getDocumentUrl(),
            item.getOcrData()
        );
    }
}
