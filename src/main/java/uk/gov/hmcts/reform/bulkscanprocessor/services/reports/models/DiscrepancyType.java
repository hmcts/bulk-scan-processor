package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

public enum DiscrepancyType {
    REPORTED_BUT_NOT_RECEIVED("reported but not received"),
    RECEIVED_BUT_NOT_REPORTED("received but not reported"),
    PAYMENT_DCNS_MISMATCH("payment dcns mismatch"),
    SCANNABLE_DOCUMENT_DCNS_MISMATCH("scannable document dcns mismatch");

    public final String text;

    DiscrepancyType(String text) {
        this.text = text;
    }
}
