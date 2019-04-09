package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentType {

    CHERISHED("cherished"),
    OTHER("other"),
    COVERSHEET("Coversheet"),
    FORM("Form");

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
