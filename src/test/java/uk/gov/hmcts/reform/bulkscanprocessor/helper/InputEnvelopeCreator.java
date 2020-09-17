package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

public final class InputEnvelopeCreator {

    private InputEnvelopeCreator() {
        // util class
    }

    public static InputEnvelope inputEnvelope(String jurisdiction) {
        return inputEnvelope(jurisdiction, "poBox", Classification.EXCEPTION, emptyList(), emptyList());
    }

    public static InputEnvelope inputEnvelope(String jurisdiction, String poBox) {
        return inputEnvelope(jurisdiction, poBox, Classification.EXCEPTION, emptyList(), emptyList());
    }

    public static InputEnvelope inputEnvelope(
        String jurisdiction,
        String poBox,
        Classification classification,
        List<InputScannableItem> scannableItems
    ) {
        return inputEnvelope(
            jurisdiction,
            poBox,
            classification,
            scannableItems,
            emptyList()
        );
    }

    public static InputEnvelope inputEnvelope(
        String jurisdiction,
        String poBox,
        Classification classification,
        List<InputScannableItem> scannableItems,
        List<InputPayment> payments
    ) {
        return new InputEnvelope(
            poBox,
            jurisdiction,
            null,
            null,
            null,
            "file.zip",
            null,
            "case_number",
            "previous_service_case_ref",
            classification,
            scannableItems,
            payments,
            emptyList()
        );
    }

    public static InputScannableItem scannableItem(String fileName) {
        return scannableItem(fileName, InputDocumentType.OTHER);
    }

    public static InputScannableItem scannableItem(String fileName, InputDocumentType documentType) {
        return scannableItem(fileName, UUID.randomUUID().toString(), documentType, new InputOcrData());
    }

    public static InputScannableItem scannableItem(String fileName, String dcn) {
        return scannableItem(fileName, dcn, InputDocumentType.OTHER, new InputOcrData());
    }

    public static InputScannableItem scannableItem(InputDocumentType documentType, InputOcrData ocrData) {
        return scannableItem("file.pdf", UUID.randomUUID().toString(), documentType, ocrData);
    }

    public static InputScannableItem scannableItem(
        String fileName,
        String dcn,
        InputDocumentType documentType,
        InputOcrData ocrData
    ) {
        return new InputScannableItem(
            dcn,
            null,
            "ocr_accuracy",
            "manula_intervention",
            "next_action",
            null,
            ocrData,
            fileName,
            "notes",
            documentType,
            null
        );
    }

    public static InputPayment payment(String documentControlNumber) {
        return new InputPayment(documentControlNumber);
    }
}
