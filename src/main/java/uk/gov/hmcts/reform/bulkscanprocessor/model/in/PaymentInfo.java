package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInfo {

    @JsonProperty("document_control_number")
    @NotEmpty(message = "Payment DCN can't be empty")
    public final String documentControlNumber;

    public PaymentInfo(@JsonProperty(value = "document_control_number", required = true) String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }
}
