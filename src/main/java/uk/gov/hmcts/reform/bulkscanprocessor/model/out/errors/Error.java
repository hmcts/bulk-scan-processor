package uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Error {

    @JsonProperty("message")
    public final String message;

    public Error(String message) {
        this.message = message;
    }
}
