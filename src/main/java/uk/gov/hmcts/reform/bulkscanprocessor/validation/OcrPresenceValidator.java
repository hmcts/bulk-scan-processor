package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.FORM;

@Component
public class OcrPresenceValidator {

    public static final String MULTIPLE_OCR_MSG = "Multiple docs with OCR";
    public static final String MISSING_OCR_MSG = "Empty OCR on 'form' document";
    public static final String MISPLACED_OCR = "OCR on document of invalid type";

    /**
     * Checks whether OCR data is on the correct document.
     *
     * @return Document with OCR.
     */
    public Optional<InputScannableItem> assertHasProperlySetOcr(List<InputScannableItem> docs) {

        throwIf(docs.stream().filter(it -> it.ocrData != null).count() > 1, MULTIPLE_OCR_MSG);
        throwIf(docs.stream().anyMatch(it -> it.documentType != FORM && it.ocrData != null), MISPLACED_OCR);
        throwIf(docs.stream().anyMatch(it -> it.documentType == FORM && it.ocrData == null), MISSING_OCR_MSG);

        return docs
            .stream()
            .filter(it -> it.ocrData != null)
            .findFirst();
    }

    private void throwIf(boolean condition, String msg) {
        if (condition) {
            throw new OcrPresenceException(msg);
        }
    }
}
