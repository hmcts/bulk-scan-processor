package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the response for SAS token.
 */
public class SasTokenResponse {
    @Schema(
        name = "SAS Token",
        description = "Shared access token to access blob"
    )
    @JsonProperty("sas_token")
    public final String sasToken;

    /**
     * Constructor for SasTokenResponse.
     * @param sasToken SAS token
     */
    public SasTokenResponse(String sasToken) {
        this.sasToken = sasToken;
    }
}
