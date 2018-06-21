package uk.gov.hmcts.reform.bulkscanning.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class SasTokenResponse {
    @ApiModelProperty(
        name = "SAS Token",
        notes = "Shared access token to access blob"
    )
    @JsonProperty("sas_token")
    public final String sasToken;

    public SasTokenResponse(String sasToken) {
        this.sasToken = sasToken;
    }
}
