package uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an error response.
 */
public class ErrorResponse {

    @JsonProperty("message")
    public final String message;

    /**
     * Constructor.
     * @param message error message
     */
    public ErrorResponse(String message) {
        this.message = message;
    }
}
