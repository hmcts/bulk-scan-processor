package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInfo {
    @JsonProperty("document_control_number")
    @NotEmpty(message = "Document control number is empty or null")
    private String documentControlNumber;

    public PaymentInfo(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }

    private PaymentInfo() {
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public void setDocumentControlNumber(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }
}
