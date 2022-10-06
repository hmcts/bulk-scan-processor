package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

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
    DEATH_CERTIFICATE("Death Certificate");

    private final String value;

    InputDocumentType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
