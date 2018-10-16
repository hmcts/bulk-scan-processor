package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class Document {

    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(Document.class);

    @JsonIgnore
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    public final List<Map<String, Object>> ocrData;

    // region constructor
    private Document(
        String fileName,
        String controlNumber,
        String type,
        Instant scannedAt,
        String url,
        String ocrData
    ) {
        this.fileName = fileName;
        this.controlNumber = controlNumber;
        this.type = type;
        this.scannedAt = scannedAt;
        this.url = url;
        this.ocrData = ocrData == null ? emptyList() : parseOcrData(ocrData);
    }
    // endregion

    private List<Map<String, Object>> parseOcrData(String ocrData) {
        byte[] decoded = Base64.getDecoder().decode(ocrData);
        TypeFactory factory = MAPPER.getTypeFactory();

        try {
            return MAPPER.readValue(
                decoded,
                factory.constructCollectionLikeType(
                    List.class,
                    factory.constructMapType(Map.class, String.class, Object.class)
                )
            );
        } catch (IOException exception) {
            log.error(exception.getMessage(), exception);

            return emptyList();
        }
    }

    public static Document fromScannableItem(ScannableItem item) {
        return new Document(
            item.getFileName(),
            item.getDocumentControlNumber(),
            item.getDocumentType(),
            item.getScanningDate().toInstant(),
            item.getDocumentUrl(),
            item.getOcrData()
        );
    }
}
