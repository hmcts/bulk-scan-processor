package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;

import java.util.List;

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
            classification,
            scannableItems,
            emptyList(),
            emptyList()
        );
    }

    public static InputScannableItem scannableItem(String fileName) {
        return scannableItem(fileName, InputDocumentType.OTHER, new OcrData());
    }

    public static InputScannableItem scannableItem(InputDocumentType documentType, OcrData ocrData) {
        return scannableItem("file.pdf", documentType, ocrData);
    }

    public static InputScannableItem scannableItem(
        String fileName,
        InputDocumentType documentType,
        OcrData ocrData
    ) {
        return new InputScannableItem(
            "control_number",
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
