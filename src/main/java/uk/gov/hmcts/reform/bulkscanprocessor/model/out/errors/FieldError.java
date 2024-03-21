package uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an error response.
 */
public class FieldError {

    @JsonProperty("field_name")
    public final String fieldName;

    @JsonProperty("message")
    public final String message;

    /**
     * Constructor.
     * @param fieldName name of the field
     * @param message error message
     */
    public FieldError(String fieldName, String message) {
        this.fieldName = fieldName;
        this.message = message;
    }

    /**
     * Returns the string representation of the object.
     */
    @Override
    public String toString() {
        return "FieldError{fieldName='" + fieldName + "', message='" + message + "'}";
    }
}
