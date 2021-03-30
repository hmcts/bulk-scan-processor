package uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors;

import java.util.List;

public class ModelValidationError {

    public final List<FieldError> errors;

    public ModelValidationError(List<FieldError> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "ModelValidationError{errors=" + errors + "}";
    }
}
