package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class DocumentTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void fromScannableItem_maps_to_document_correctly() {
        ScannableItem scannableItem = scannableItem(DocumentType.CHERISHED);
        Document document = fromScannableItem(scannableItem);

        assertThat(document.controlNumber).isEqualTo(scannableItem.getDocumentControlNumber());
        assertThat(document.fileName).isEqualTo(scannableItem.getFileName());
        assertThat(document.scannedAt).isEqualTo(scannableItem.getScanningDate());
        assertThat(document.subtype).isEqualTo(scannableItem.getDocumentSubtype());
        assertThat(document.type).isEqualTo(scannableItem.getDocumentType());
        assertThat(document.uuid).isEqualTo(scannableItem.getDocumentUuid());
    }

    @Test
    void fromScannableItem_should_serialize_instant_one_zero() throws JsonProcessingException {
        Instant scanningDate = Instant.parse("2022-05-23T01:02:03.120Z");
        ScannableItem scannableItem = scannableItem(DocumentType.COVERSHEET, scanningDate);

        Document document = fromScannableItem(scannableItem);

        String documentSerialized = objectMapper.writeValueAsString(document);
        assertThat(documentSerialized.contains("2022-05-23T01:02:03.120Z")).isTrue();
    }

    @Test
    void fromScannableItem_should_serialize_instant_two_zeroes() throws JsonProcessingException {
        Instant scanningDate = Instant.parse("2022-05-23T01:02:03.100Z");
        ScannableItem scannableItem = scannableItem(DocumentType.COVERSHEET, scanningDate);

        Document document = fromScannableItem(scannableItem);

        String documentSerialized = objectMapper.writeValueAsString(document);
        assertThat(documentSerialized.contains("2022-05-23T01:02:03.100Z")).isTrue();
    }

    @Test
    void fromScannableItem_should_serialize_instant_three_zeroes() throws JsonProcessingException {
        Instant scanningDate = Instant.parse("2022-05-23T01:02:03.000Z");
        ScannableItem scannableItem = scannableItem(DocumentType.COVERSHEET, scanningDate);

        Document document = fromScannableItem(scannableItem);

        String documentSerialized = objectMapper.writeValueAsString(document);
        assertThat(documentSerialized.contains("2022-05-23T01:02:03.000Z")).isTrue();
    }

    @Test
    void fromScannableItem_should_serialize_instant_all_zeroes() throws JsonProcessingException {
        Instant scanningDate = Instant.parse("2022-05-23T00:00:00.000Z");
        ScannableItem scannableItem = scannableItem(DocumentType.COVERSHEET, scanningDate);

        Document document = fromScannableItem(scannableItem);

        String documentSerialized = objectMapper.writeValueAsString(document);
        assertThat(documentSerialized.contains("2022-05-23T00:00:00.000Z")).isTrue();
    }

    private ScannableItem scannableItem(DocumentType documentType) {
        return scannableItem(documentType, Instant.now());
    }

    private ScannableItem scannableItem(DocumentType documentType, Instant scanningDate) {
        OcrData ocrData = new OcrData(singletonList(new OcrDataField(new TextNode("ocr"), new TextNode("data1"))));

        ScannableItem scannableItem = new ScannableItem(
            "1111001",
            scanningDate,
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
