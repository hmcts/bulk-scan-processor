package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the status of a jurisdiction configuration.
 */
public class JurisdictionConfigurationStatus {

    public final String jurisdiction;

    @JsonProperty("is_correct")
    public final boolean isCorrect;

    @JsonProperty("error_description")
    public final String errorDescription;

    @JsonProperty("error_response_status")
    public final Integer errorResponseStatus;

    /**
     * Constructor for JurisdictionConfigurationStatus.
     * @param jurisdiction the jurisdiction
     * @param isCorrect whether the configuration is correct
     * @param errorDescription the error description
     * @param errorResponseStatus the error response status
     */
    public JurisdictionConfigurationStatus(
        String jurisdiction,
        boolean isCorrect,
        String errorDescription,
        Integer errorResponseStatus
    ) {
        this.jurisdiction = jurisdiction;
        this.isCorrect = isCorrect;
        this.errorDescription = errorDescription;
        this.errorResponseStatus = errorResponseStatus;
    }

    /**
     * Creates a new instance of the class.
     * @param jurisdiction the jurisdiction
     * @param isCorrect whether the configuration is correct
     */
    public JurisdictionConfigurationStatus(
        String jurisdiction,
        boolean isCorrect
    ) {
        this(jurisdiction, isCorrect, null, null);
    }

    /**
     * Checks if the error response status is a client error.
     * @return boolean indicating if the error response status is a client error
     */
    public boolean isClientError() {
        return errorResponseStatus != null
            && errorResponseStatus >= 400
            && errorResponseStatus < 500;
    }
}
