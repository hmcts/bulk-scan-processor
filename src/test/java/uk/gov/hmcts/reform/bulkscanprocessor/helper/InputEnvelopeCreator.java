package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public final class InputEnvelopeCreator {

    private InputEnvelopeCreator() {
        // util class
    }

    public static InputEnvelope inputEnvelope(String jurisdiction) {
        return inputEnvelope(jurisdiction, Classification.EXCEPTION, emptyList());
    }

    public static InputEnvelope inputEnvelope(
        String jurisdiction,
        Classification classification,
        List<InputScannableItem> scannableItems
    ) {
        return new InputEnvelope(
            "poBox",
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

    public static InputScannableItem scannableItem(InputDocumentType documentType, Map<String, String> ocrData) {
        return new InputScannableItem(
            "control_number",
            null,
            "ocr_accuracy",
            "manula_intervention",
            "next_action",
            null,
            ocrData,
            "file.pdf",
            "notes",
            documentType
        );
    }

}
