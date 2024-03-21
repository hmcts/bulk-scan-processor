package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the type of the input document.
 */
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

    /**
     * Constructor.
     * @param value the value of the enum
     */
    InputDocumentType(String value) {
        this.value = value;
    }

    /**
     * Returns the value of the enum.
     * @return the value of the enum
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * toString method.
     * @return the value of the enum
     */
    @Override
    public String toString() {
        return value;
    }
}
