package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InputPayment {

    public final String documentControlNumber;

    public InputPayment(
        @JsonProperty("document_control_number") String documentControlNumber
    ) {
        this.documentControlNumber = documentControlNumber;
    }
}
