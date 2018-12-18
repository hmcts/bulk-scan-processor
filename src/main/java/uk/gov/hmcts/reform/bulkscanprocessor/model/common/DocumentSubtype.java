package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

public enum DocumentSubtype {
    SSCS1("sscs1");

    private final String value;

    DocumentSubtype(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
