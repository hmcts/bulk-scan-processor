package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class EnvelopeCreator {

    private static final MetafileJsonValidator validator;

    static {
        try {
            validator = new MetafileJsonValidator();
        } catch (IOException | ProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    private EnvelopeCreator() {
    }

    public static InputStream getMetaFile() {
        return EnvelopeCreator.class.getResourceAsStream("/metafiles/valid/from-spec.json");
    }

    public static InputEnvelope getEnvelopeFromMetafile() throws IOException {
        try (InputStream inputStream = getMetaFile()) {
            return validator.parseMetafile(IOUtils.toByteArray(inputStream));
        }
    }

    public static InputEnvelope getEnvelopeFromMetafile(byte[] metafile) throws IOException {
        return validator.parseMetafile(metafile);
    }

    public static List<EnvelopeResponse> envelopeResponses() throws Exception {
        return EnvelopeResponseMapper.toEnvelopesResponse(envelopes());
    }

    public static EnvelopeResponse envelopeResponse() throws Exception {
        return EnvelopeResponseMapper.toEnvelopeResponse(envelope());
    }

    public static List<Envelope> envelopes() throws Exception {
        return ImmutableList.of(envelope());
    }

    public static Envelope envelope() {
        return envelope("SSCS", Status.PROCESSED);
    }

    public static Envelope envelope(String jurisdiction, Status status) {
        return envelope(UUID.randomUUID() + ".zip", jurisdiction, status);
    }

    public static Envelope envelope(String zipFileName, String jurisdiction, Status status) {
        Timestamp timestamp = getTimestamp();

        Envelope envelope = new Envelope(
            "SSCSPO",
            jurisdiction,
            timestamp,
            timestamp,
            timestamp,
            zipFileName,
            "1111222233334446",
            Classification.EXCEPTION,
            scannableItems(),
            payments(),
            nonScannableItems(),
            "SSCS"
        );

        envelope.setStatus(status);

        return envelope;
    }

    public static InputScannableItem inputScannableItem(InputDocumentType docType) {
        return new InputScannableItem(
            "control number",
            getTimestamp(),
            "ocr accuracy",
            "manual intervention",
            "next action",
            getTimestamp(),
            new OcrData(),
            "file.pdf",
            "notes",
            docType
        );
    }

    public static Envelope envelopeNotified() {
        return envelope("SSCS", Status.NOTIFICATION_SENT);
    }

    private static List<ScannableItem> scannableItems() {
        Timestamp timestamp = getTimestamp();

        ScannableItem scannableItem1 = new ScannableItem(
            "1111001",
            timestamp,
            "test",
            "test",
            "return",
            timestamp,
            null,
            "1111001.pdf",
            "test",
            DocumentType.CHERISHED,
            null
        );
        scannableItem1.setDocumentUrl("http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebceb");

        ScannableItem scannableItem2 = new ScannableItem(
            "1111002",
            timestamp,
            "test",
            "test",
            "return",
            timestamp,
            ocrData(ImmutableMap.of("name1", "value1")),
            "1111002.pdf",
            "test",
            DocumentType.OTHER,
            DocumentSubtype.SSCS1
        );
        scannableItem2.setDocumentUrl("http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe");

        return ImmutableList.of(scannableItem1, scannableItem2);
    }

    private static OcrData ocrData(Map<String, String> data) {
        OcrData ocrData = new OcrData();
        List<OcrDataField> ocrDataFields = data.entrySet()
            .stream()
            .map(
                e -> new OcrDataField(new TextNode(e.getKey()), new TextNode(e.getValue()))
            )
            .collect(Collectors.toList());

        ocrData.setFields(ocrDataFields);

        return ocrData;
    }

    private static List<NonScannableItem> nonScannableItems() {
        return ImmutableList.of(
            new NonScannableItem("1111001", "CD", "4GB USB memory stick")
        );
    }

    private static List<Payment> payments() {
        return ImmutableList.of(
            new Payment(
                "1111002",
                "Cheque",
                BigDecimal.valueOf(100),
                "GBP",
                "1000000000",
                "112233",
                "12345678"
            )
        );
    }

    private static Timestamp getTimestamp() {
        return Timestamp.from(Instant.parse("2018-06-23T12:34:56.123Z"));
    }
}
