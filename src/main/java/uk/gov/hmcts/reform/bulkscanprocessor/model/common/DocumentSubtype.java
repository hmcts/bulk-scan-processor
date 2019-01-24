package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentSubtype {
    WILL("will"),
    SSCS1("sscs1"),
    COVERSHEET("coversheet");

    private final String value;

    DocumentSubtype(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
