package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.FORM;

@Component
public class EnvelopeOcrPresenceValidator {

    public static final String MULTIPLE_OCR_MSG = "Multiple docs with OCR";
    public static final String MISSING_OCR_MSG = "Empty OCR on 'form' document";
    public static final String MISPLACED_OCR_MSG = "OCR on document of invalid type";
    public static final String MISSING_DOC_SUBTYPE_MSG = "Missing subtype on document with OCR";

    // The only document types that can (and must) have OCR data.
    protected static final List<DocumentType> OCR_DOC_TYPES = ImmutableList.of(FORM);

    /**
     * Checks whether OCR data is on the correct document.
     *
     * @return Document with OCR.
     */
    public Optional<ScannableItem> assertHasProperlySetOcr(List<ScannableItem> docs) {

        if (docs.stream().filter(doc -> doc.getOcrData() != null).count() > 1) {
            throw new OcrPresenceException(MULTIPLE_OCR_MSG);
        }
        if (docs.stream().anyMatch(doc -> !OCR_DOC_TYPES.contains(doc.getDocumentType()) && doc.getOcrData() != null)) {
            throw new OcrPresenceException(MISPLACED_OCR_MSG);
        }
        if (docs.stream().anyMatch(doc -> OCR_DOC_TYPES.contains(doc.getDocumentType()) && doc.getOcrData() == null)) {
            throw new OcrPresenceException(MISSING_OCR_MSG);
        }
        // TODO: Looks like this check is not necessary as documentSubtype is never null
        if (docs.stream().anyMatch(
            doc -> doc.getDocumentSubtype() == null
                && doc.getOcrData() != null
        )) {
            throw new OcrPresenceException(MISSING_DOC_SUBTYPE_MSG);
        }

        return docs
            .stream()
            .filter(it -> it.getOcrData() != null)
            .findFirst();
    }
}
