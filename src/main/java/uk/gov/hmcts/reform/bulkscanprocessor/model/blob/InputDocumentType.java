package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonValue;

public enum InputDocumentType {
    CHERISHED("Cherished"),
    OTHER("Other"),
    WILL("Will"),
    SSCS1("SSCS1"),
    FORM("Form"),
    COVERSHEET("Coversheet"),
    SUPPORTING_DOCUMENTS("Supporting Documents"),
    FORENSIC_SHEETS("Forensic Sheets"),
    IHT("IHT"),
    PPS_LEGAL_STATEMENT("PP's Legal Statement"),
    PPS_LEGAL_STATEMENT_WITHOUT_APOSTROPHE("PPs Legal Statement");

    private final String value;

    InputDocumentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
