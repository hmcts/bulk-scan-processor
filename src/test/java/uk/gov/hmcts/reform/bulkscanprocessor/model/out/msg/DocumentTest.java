package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Document.fromScannableItem;

public class DocumentTest {

    @Test
    public void fromScannableItem_maps_to_document_correctly() {
        ScannableItem scannableItem = scannableItem(DocumentType.CHERISHED);
        Document document = fromScannableItem(scannableItem);

        assertThat(document.controlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(document.fileName).isEqualTo(scannableItem.getFileName());
        assertThat(document.scannedAt).isEqualTo(scannableItem.getScanningDate());
        assertThat(document.subtype).isEqualTo(scannableItem.getDocumentSubtype());
        assertThat(document.type).isEqualTo(scannableItem.getDocumentType());
        assertThat(document.uuid).isEqualTo(scannableItem.getDocumentUuid());
    }

    private ScannableItem scannableItem(DocumentType documentType) {
        OcrData ocrData = new OcrData(singletonList(new OcrDataField(new TextNode("ocr"), new TextNode("data1"))));

        ScannableItem scannableItem = new ScannableItem(
            "1111001",
            Instant.now(),
            "ocrAccuracy1",
            "manualIntervention1",
            "nextAction1",
            Instant.now().plus(1, DAYS),
            ocrData,
            "fileName1.pdf",
            "notes 1",
            documentType,
            DocumentSubtype.SSCS1,
            new String[]{"warning 1"}
        );

        scannableItem.setDocumentUuid("5fef5f98-e875-4084-b115-47188bc9066b");
        return scannableItem;
    }
}
