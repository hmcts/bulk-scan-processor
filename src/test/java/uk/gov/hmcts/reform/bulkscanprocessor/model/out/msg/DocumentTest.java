package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Document.fromScannableItem;

public class DocumentTest {

    private static final String DOCUMENT_TYPE_CHERISHED = "Cherished";
    private static final String CCD_DOCUMENT_TYPE_CHERISHED = "cherished";

    private static final String CCD_DOCUMENT_TYPE_OTHER = "other";

    @Test
    public void fromScannableItem_maps_to_document_correctly() {
        ScannableItem scannableItem = scannableItem(DOCUMENT_TYPE_CHERISHED);
        Document document = fromScannableItem(scannableItem);

        assertThat(document.controlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(document.fileName).isEqualTo(scannableItem.getFileName());
        assertThat(document.scannedAt).isEqualTo(scannableItem.getScanningDate().toInstant());
        assertThat(document.type).isEqualTo(CCD_DOCUMENT_TYPE_CHERISHED);
        assertThat(document.url).isEqualTo(scannableItem.getDocumentUrl());
    }

    @Test
    public void fromScannableItem_maps_document_type_correctly() {
        Map<String, String> expectedTypes = new HashMap<>();
        expectedTypes.put("Cherished", CCD_DOCUMENT_TYPE_CHERISHED);
        expectedTypes.put("cherished", CCD_DOCUMENT_TYPE_CHERISHED);
        expectedTypes.put("CHERISHED", CCD_DOCUMENT_TYPE_CHERISHED);
        expectedTypes.put("Other", CCD_DOCUMENT_TYPE_OTHER);
        expectedTypes.put("other", CCD_DOCUMENT_TYPE_OTHER);
        expectedTypes.put("OTHER", CCD_DOCUMENT_TYPE_OTHER);
        expectedTypes.put("SSCS1", CCD_DOCUMENT_TYPE_OTHER);
        expectedTypes.put("sscs1", CCD_DOCUMENT_TYPE_OTHER);
        expectedTypes.put("Sscs1", CCD_DOCUMENT_TYPE_OTHER);


        for (Map.Entry<String, String> entry : expectedTypes.entrySet()) {
            String inputDocumentType = entry.getKey();
            String expectedCcdType = entry.getValue();

            ScannableItem scannableItem = scannableItem(inputDocumentType);
            assertThat(Document.fromScannableItem(scannableItem).type)
                .as(String.format("Expected CCD document type for '%s'", inputDocumentType))
                .isEqualTo(expectedCcdType);
        }
    }

    private ScannableItem scannableItem(String documentType) {
        ScannableItem scannableItem = new ScannableItem(
            "1111001",
            Timestamp.from(Instant.now()),
            "ocrAccuracy1",
            "manualIntervention1",
            "nextAction1",
            Timestamp.from(Instant.now().plus(1, DAYS)),
            ImmutableMap.of("ocr", "data1"),
            "fileName1.pdf",
            "notes 1",
            documentType
        );

        scannableItem.setDocumentUrl("http://document-url.example.com");
        return scannableItem;
    }
}
