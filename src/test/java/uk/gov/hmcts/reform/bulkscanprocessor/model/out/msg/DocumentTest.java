package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Document.fromScannableItem;

public class DocumentTest {

    private static final String DOCUMENT_TYPE_CHERISHED = "Cherished";
    private static final String CCD_DOCUMENT_TYPE_CHERISHED = "cherished";

    private static final String DOCUMENT_TYPE_OTHER = "Other";
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
        assertThat(documentsFromScannableItems("Cherished", "cherished", "CHERISHED"))
            .extracting("type")
            .containsOnly(CCD_DOCUMENT_TYPE_CHERISHED);

        assertThat(documentsFromScannableItems("Other", "other", "OTHER"))
            .extracting("type")
            .containsOnly(CCD_DOCUMENT_TYPE_OTHER);

        assertThat(documentsFromScannableItems("SSCS1", "sscs1", "Sscs1"))
            .extracting("type")
            .containsOnly(CCD_DOCUMENT_TYPE_OTHER);
    }

    private List<Document> documentsFromScannableItems(String... inputDocumentTypes) {
        return Arrays.stream(inputDocumentTypes)
            .map(type -> fromScannableItem(scannableItem(type)))
            .collect(toList());
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
            documentType,
            "sscs1"
        );

        scannableItem.setDocumentUrl("http://document-url.example.com");
        return scannableItem;
    }
}
