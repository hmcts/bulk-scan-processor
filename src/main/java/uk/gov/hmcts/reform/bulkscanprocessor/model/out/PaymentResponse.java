package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentResponse {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    public PaymentResponse(
        @JsonProperty("document_control_number") String documentControlNumber
    ) {
        this.documentControlNumber = documentControlNumber;
    }
}
