package uk.gov.hmcts.reform.bulkscanprocessor.validation.model;

import java.util.List;

/**
 * Represents warnings that occurred during OCR validation.
 */
public class OcrValidationWarnings {

    public final String documentControlNumber;
    public final List<String> warnings;

    /**
     * Constructor for OcrValidationWarnings.
     * @param documentControlNumber document control number of the document that was validated
     * @param warnings list of warnings that occurred during OCR validation
     */
    public OcrValidationWarnings(String documentControlNumber, List<String> warnings) {
        this.documentControlNumber = documentControlNumber;
        this.warnings = warnings;
    }
}
