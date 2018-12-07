package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.sql.Timestamp;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Document.fromScannableItem;

public class DocumentTest {

    private static final String DOCUMENT_TYPE_CHERISHED = "Cherished";
    private static final String DOCUMENT_TYPE_OTHER = "Other";

    @Test
    public void fromScannableItem_maps_to_document_correctly() {
        ScannableItem scannableItem = scannableItem(DOCUMENT_TYPE_CHERISHED);
        Document document = fromScannableItem(scannableItem);

        assertThat(document.controlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(document.fileName).isEqualTo(scannableItem.getFileName());
        assertThat(document.ocrData).isEqualTo(scannableItem.getOcrData());
        assertThat(document.scannedAt).isEqualTo(scannableItem.getScanningDate().toInstant());
        assertThat(document.type).isEqualTo(scannableItem.getDocumentType());
        assertThat(document.url).isEqualTo(scannableItem.getDocumentUrl());
    }

    @Test
    public void fromScannableItem_maps_document_type_correctly() {
        ScannableItem cherishedScannableItem = scannableItem(DOCUMENT_TYPE_CHERISHED);
        ScannableItem otherScannableItem = scannableItem(DOCUMENT_TYPE_OTHER);
        ScannableItem sscs1ScannableItem = scannableItem("SSCS1");

        assertThat(fromScannableItem(cherishedScannableItem).type).isEqualTo(DOCUMENT_TYPE_CHERISHED);
        assertThat(fromScannableItem(otherScannableItem).type).isEqualTo(DOCUMENT_TYPE_OTHER);
        assertThat(fromScannableItem(sscs1ScannableItem).type).isEqualTo(DOCUMENT_TYPE_OTHER);
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
