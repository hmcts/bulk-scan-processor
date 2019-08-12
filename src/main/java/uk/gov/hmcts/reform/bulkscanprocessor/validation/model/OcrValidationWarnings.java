package uk.gov.hmcts.reform.bulkscanprocessor.validation.model;

import java.util.List;

public class OcrValidationWarnings {

    public final String documentControlNumber;
    public final List<String> warnings;

    public OcrValidationWarnings(String documentControlNumber, List<String> warnings) {
        this.documentControlNumber = documentControlNumber;
        this.warnings = warnings;
    }
}
