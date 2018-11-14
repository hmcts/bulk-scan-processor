package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class SasQueryParamsResponse {
    @ApiModelProperty(
        name = "SAS Token",
        notes = "Shared access token to access blob"
    )
    @JsonProperty("sas_token")
    public final String sasQueryParams;

    public SasQueryParamsResponse(String sasQueryParams) {
        this.sasQueryParams = sasQueryParams;
    }
}
