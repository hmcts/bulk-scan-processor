package uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors;

import java.util.List;

/**
 * Represents an error response.
 */
public class ModelValidationError {

    public final List<FieldError> errors;

    /**
     * Constructor.
     * @param errors list of field errors
     */
    public ModelValidationError(List<FieldError> errors) {
        this.errors = errors;
    }

    /**
     * Returns the string representation of the object.
     */
    @Override
    public String toString() {
        return "ModelValidationError{errors=" + errors + "}";
    }
}
