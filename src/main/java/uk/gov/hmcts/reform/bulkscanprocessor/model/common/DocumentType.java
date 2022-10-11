package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentType {

    CHERISHED("cherished"),
    OTHER("other"),
    COVERSHEET("coversheet"),
    FORM("form"),
    SUPPORTING_DOCUMENTS("supporting_documents"),
    WILL("will"),
    FORENSIC_SHEETS("forensic_sheets"),
    IHT("IHT"),
    PPS_LEGAL_STATEMENT("pps_legal_statement"),
    DEATH_CERTIFICATE("death_certificate");

    private final String value;

    DocumentType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
