package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a payment extracted from OCR.
 */
public class Payment {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    /**
     * Constructor for Payment.
     * @param documentControlNumber payment document control number
     */
    public Payment(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }
}
