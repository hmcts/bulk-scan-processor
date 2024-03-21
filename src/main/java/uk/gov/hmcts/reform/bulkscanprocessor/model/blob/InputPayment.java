package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a payment in a document.
 */
public class InputPayment {

    public final String documentControlNumber;

    /**
     * Constructor for InputPayment.
     * @param documentControlNumber the document control number
     */
    public InputPayment(
        @JsonProperty("document_control_number") String documentControlNumber
    ) {
        this.documentControlNumber = documentControlNumber;
    }
}
