package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentType {

    CHERISHED("cherished"),
    OTHER("other");

    private final String value;

    DocumentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String toString() {
        return value;
    }
}
