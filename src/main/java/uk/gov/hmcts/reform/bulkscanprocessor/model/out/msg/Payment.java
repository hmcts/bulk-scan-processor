package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Payment {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    public Payment(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }
}
