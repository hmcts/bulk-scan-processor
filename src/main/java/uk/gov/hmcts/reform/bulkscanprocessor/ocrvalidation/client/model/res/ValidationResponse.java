package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the response from the OCR validation service.
 */
public class ValidationResponse {

    public final Status status;
    public final List<String> warnings;
    public final List<String> errors;

    /**
     * Constructor for ValidationResponse.
     * @param status status of the validation
     * @param warnings warnings from the validation
     * @param errors errors from the validation
     */
    public ValidationResponse(
        @JsonProperty("status") Status status,
        @JsonProperty("warnings") List<String> warnings,
        @JsonProperty("errors") List<String> errors
    ) {
        this.status = status;
        this.warnings = warnings;
        this.errors = errors;
    }
}
