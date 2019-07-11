package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
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
        return inputEnvelope(jurisdiction, "poBox", Classification.EXCEPTION, emptyList());
    }

    public static InputEnvelope inputEnvelope(String jurisdiction, String poBox) {
        return inputEnvelope(jurisdiction, poBox, Classification.EXCEPTION, emptyList());
    }

    public static InputEnvelope inputEnvelope(
        String jurisdiction,
        String poBox,
        Classification classification,
        List<InputScannableItem> scannableItems
    ) {
        return new InputEnvelope(
            poBox,
            jurisdiction,
            null,
            null,
            null,
            "file.zip",
            "case_number",
            "previous_service_case_ref",
            classification,
            scannableItems,
            emptyList(),
            emptyList()
        );
    }

    public static InputScannableItem scannableItem(String fileName) {
        return scannableItem(fileName, UUID.randomUUID().toString(), InputDocumentType.OTHER, new InputOcrData());
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

}
