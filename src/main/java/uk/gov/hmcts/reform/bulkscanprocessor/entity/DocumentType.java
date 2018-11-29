package uk.gov.hmcts.reform.bulkscanprocessor.entity;

public enum DocumentType {

    CHERISHED("Cherished"),
    OTHER("Other");

    private final String value;

    DocumentType(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
