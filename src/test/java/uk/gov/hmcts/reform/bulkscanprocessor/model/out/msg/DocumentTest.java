package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Document.fromScannableItem;

public class DocumentTest {

    private static final String CCD_DOCUMENT_TYPE_CHERISHED = "cherished";
    private static final String CCD_DOCUMENT_TYPE_OTHER = "other";

    @Test
    public void fromScannableItem_maps_to_document_correctly() {
        String documentType = "cherished";
        ScannableItem scannableItem = scannableItem(documentType);
        Document document = fromScannableItem(scannableItem);

        assertThat(document.controlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(document.fileName).isEqualTo(scannableItem.getFileName());
        assertThat(document.scannedAt).isEqualTo(scannableItem.getScanningDate().toInstant());
        assertThat(document.type).isEqualTo(documentType);
        assertThat(document.url).isEqualTo(scannableItem.getDocumentUrl());
    }

    @Test
    public void fromScannableItem_maps_document_type_correctly() {
        Map<String, String> expectedTypes = ImmutableMap.of(
            "cherished", CCD_DOCUMENT_TYPE_CHERISHED,
            "other", CCD_DOCUMENT_TYPE_OTHER,
            "sscs1", CCD_DOCUMENT_TYPE_OTHER);

        expectedTypes.forEach((inputDocumentType, expectedCcdType) -> {
            ScannableItem scannableItem = scannableItem(inputDocumentType);
            assertThat(Document.fromScannableItem(scannableItem).type)
                .as(String.format("Expected CCD document type for '%s'", inputDocumentType))
                .isEqualTo(expectedCcdType);
        });
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
