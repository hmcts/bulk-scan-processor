package uk.gov.hmcts.reform.bulkscanprocessor.services.idam;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JurisdictionConfigurationStatus {

    public final String jurisdiction;

    @JsonProperty("is_correct")
    public final boolean isCorrect;

    @JsonProperty("error_description")
    public final String errorDescription;

    @JsonProperty("error_response_status")
    public final Integer errorResponseStatus;

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

    public JurisdictionConfigurationStatus(
        String jurisdiction,
        boolean isCorrect
    ) {
        this(jurisdiction, isCorrect, null, null);
    }

    public boolean isClientError() {
        return errorResponseStatus != null
            && errorResponseStatus >= 400
            && errorResponseStatus < 500;
    }
}
