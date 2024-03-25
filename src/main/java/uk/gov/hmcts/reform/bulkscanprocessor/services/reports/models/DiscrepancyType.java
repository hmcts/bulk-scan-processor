package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents a discrepancy in the data.
 */
public enum DiscrepancyType {

    REPORTED_BUT_NOT_RECEIVED("reported but not received"),
    RECEIVED_BUT_NOT_REPORTED("received but not reported"),
    PAYMENT_DCNS_MISMATCH("payment dcns mismatch"),
    SCANNABLE_DOCUMENT_DCNS_MISMATCH("scannable document dcns mismatch"),
    RESCAN_FOR_MISMATCH("rescan for file name mismatch"),
    REJECTED_ENVELOPE("envelope rejected");

    @JsonValue
    public final String text;

    /**
     * Constructor for the DiscrepancyType.
     * @param text The text of the discrepancy type
     */
    DiscrepancyType(String text) {
        this.text = text;
    }
}
