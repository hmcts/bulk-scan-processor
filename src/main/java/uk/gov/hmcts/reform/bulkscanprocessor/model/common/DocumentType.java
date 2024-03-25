package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Document types.
 */
public enum DocumentType {

    CHERISHED("cherished"),
    OTHER("other"),
    COVERSHEET("coversheet"),
    FORM("form"),
    SUPPORTING_DOCUMENTS("supporting_documents"),
    WILL("will"),
    FORENSIC_SHEETS("forensic_sheets"),
    IHT("iht"),
    PPS_LEGAL_STATEMENT("pps_legal_statement");

    private final String value;

    /**
     * DocumentType constructor.
     * @param value The value
     */
    DocumentType(String value) {
        this.value = value.toLowerCase();
    }

    /**
     * To string.
     * @return The value
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
