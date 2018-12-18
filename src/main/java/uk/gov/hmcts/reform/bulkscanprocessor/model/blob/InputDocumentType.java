package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

public enum InputDocumentType {
    CHERISHED("Cherished"),
    OTHER("Other"),
    SSCS1("SSCS1");

    private final String value;

    InputDocumentType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
