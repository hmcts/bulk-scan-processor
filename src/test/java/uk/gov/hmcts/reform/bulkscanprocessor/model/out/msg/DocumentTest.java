package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;

import java.sql.Timestamp;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Document.fromScannableItem;

public class DocumentTest {

    @Test
    public void fromScannableItem_maps_to_document_correctly() {
        ScannableItem scannableItem = scannableItem(DocumentType.CHERISHED);
        Document document = fromScannableItem(scannableItem);

        assertThat(document.controlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(document.fileName).isEqualTo(scannableItem.getFileName());
        assertThat(document.scannedAt).isEqualTo(scannableItem.getScanningDate().toInstant());
        assertThat(document.subtype).isEqualTo(scannableItem.getDocumentSubtype());
        assertThat(document.type).isEqualTo(scannableItem.getDocumentType());
        assertThat(document.url).isEqualTo(scannableItem.getDocumentUrl());
    }

    private ScannableItem scannableItem(DocumentType documentType) {
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
            documentType,
            DocumentSubtype.SSCS1
        );

        scannableItem.setDocumentUrl("http://document-url.example.com");
        return scannableItem;
    }
}
