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
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;

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
        return envelope(randomUUID() + ".zip", jurisdiction, status);
    }

    public static Envelope envelope(String jurisdiction, Status status, String container) {
        return envelope(randomUUID() + ".zip", jurisdiction, status, scannableItems(), container);
    }

    public static Envelope envelope(String jurisdiction, Status status, List<ScannableItem> scannableItems) {
        return envelope(randomUUID() + ".zip", jurisdiction, status, scannableItems);
    }

    public static Envelope envelope(String zipFileName, String jurisdiction, Status status) {
        return envelope(zipFileName, jurisdiction, status, scannableItems());
    }

    public static Envelope envelope(
        String zipFileName,
        String jurisdiction,
        Status status,
        List<ScannableItem> scannableItems
    ) {
        return envelope(
            zipFileName,
            jurisdiction,
            status, scannableItems,
            "SSCS"
        );
    }

    public static Envelope envelope(
        String zipFileName,
        String jurisdiction,
        Status status,
        List<ScannableItem> scannableItems,
        String container
    ) {
        Instant timestamp = getInstant();

        Envelope envelope = new Envelope(
            "SSCSPO",
            jurisdiction,
            timestamp,
            timestamp,
            timestamp,
            zipFileName,
            "1111222233334446",
            "123654789",
            Classification.EXCEPTION,
            scannableItems,
            payments(),
            nonScannableItems(),
            container
        );

        envelope.setStatus(status);

        return envelope;
    }

    public static InputScannableItem inputScannableItem(InputDocumentType docType) {
        return inputScannableItem(docType, null);
    }

    public static InputScannableItem inputScannableItem(InputDocumentType docType, String docSubtype) {
        InputOcrData inputOcrData = new InputOcrData();
        inputOcrData.setFields(Collections.emptyList());

        return new InputScannableItem(
            "control number",
            getInstant(),
            "ocr accuracy",
            "manual intervention",
            "next action",
            getInstant(),
            inputOcrData,
            "file.pdf",
            "notes",
            docType,
            docSubtype
        );
    }

    public static Envelope envelopeNotified() {
        return envelope("SSCS", Status.NOTIFICATION_SENT);
    }

    public static ScannableItem scannableItem(
        String dcn,
        OcrData ocr,
        DocumentType documentType,
        String documentSubtype
    ) {
        Instant timestamp = getInstant();

        return new ScannableItem(
            dcn,
            timestamp,
            "test",
            "test",
            "return",
            timestamp,
            ocr,
            dcn + ".pdf",
            "test",
            documentType,
            documentSubtype,
            new String[]{"warning 1"}
        );
    }

    private static List<ScannableItem> scannableItems() {
        return ImmutableList.of(
            scannableItem(randomUUID().toString(), null, DocumentType.CHERISHED, null),
            scannableItem(
                randomUUID().toString(),
                ocrData(ImmutableMap.of("name1", "value1")),
                DocumentType.OTHER,
                DocumentSubtype.SSCS1
            )
        );
    }

    public static OcrData ocrData(Map<String, String> data) {
        return new OcrData(data
            .entrySet()
            .stream()
            .map(
                e -> new OcrDataField(new TextNode(e.getKey()), new TextNode(e.getValue()))
            )
            .collect(Collectors.toList())
        );
    }

    private static List<NonScannableItem> nonScannableItems() {
        return newArrayList(
            new NonScannableItem("1111001", "CD", "4GB USB memory stick")
        );
    }

    private static List<Payment> payments() {
        return newArrayList(
            new Payment(
                "1111002"
            )
        );
    }

    private static Instant getInstant() {
        return Instant.parse("2018-06-23T12:34:56.123Z");
    }
}
