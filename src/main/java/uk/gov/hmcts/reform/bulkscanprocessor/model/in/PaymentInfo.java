package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;

/**
 * Represents payment information extracted from a document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInfo {
    @JsonProperty("document_control_number")
    @NotEmpty(message = "Document control number is empty or null")
    private String documentControlNumber;

    /**
     * Constructor.
     * @param documentControlNumber The document control number
     */
    public PaymentInfo(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }

    /**
     * Constructor.
     */
    private PaymentInfo() {
    }

    /**
     * Returns the document control number.
     * @return The document control number
     */
    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    /**
     * Sets the document control number.
     * @param documentControlNumber The document control number
     */
    public void setDocumentControlNumber(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }
}
