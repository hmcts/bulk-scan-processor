package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.FORM;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType.SSCS1;

/**
 * Validates the presence of OCR data on the correct document.
 */
@Component
public class OcrPresenceValidator {

    static final String MULTIPLE_OCR_MSG = "Multiple docs with OCR";
    static final String MISSING_OCR_MSG = "Empty OCR on 'form' document";
    static final String MISPLACED_OCR_MSG = "OCR on document of invalid type";
    static final String MISSING_DOC_SUBTYPE_MSG = "Missing subtype on document with OCR";

    // The only document types that can (and must) have OCR data.
    // Note: remove 'SSCS1' once sscs migrates to the new format.
    protected static final List<InputDocumentType> OCR_DOC_TYPES = List.of(FORM, SSCS1);

    /**
     * Checks whether OCR data is on the correct document.
     *
     * @return Document with OCR.
     */
    public Optional<InputScannableItem> assertHasProperlySetOcr(List<InputScannableItem> docs) {

        if (docs.stream().filter(doc -> doc.ocrData != null).count() > 1) {
            throw new OcrPresenceException(MULTIPLE_OCR_MSG);
        }
        if (docs.stream().anyMatch(doc -> !OCR_DOC_TYPES.contains(doc.documentType) && doc.ocrData != null)) {
            throw new OcrPresenceException(MISPLACED_OCR_MSG);
        }
        if (docs.stream().anyMatch(doc -> OCR_DOC_TYPES.contains(doc.documentType) && doc.ocrData == null)) {
            throw new OcrPresenceException(MISSING_OCR_MSG);
        }
        // TODO: For SSCS1 we don't receive document subtype as it follows a different contract
        if (docs.stream().anyMatch(
            doc -> doc.documentType != SSCS1 && doc.documentSubtype == null && doc.ocrData != null
        )) {
            throw new OcrPresenceException(MISSING_DOC_SUBTYPE_MSG);
        }

        return docs
            .stream()
            .filter(it -> it.ocrData != null)
            .findFirst();
    }
}
