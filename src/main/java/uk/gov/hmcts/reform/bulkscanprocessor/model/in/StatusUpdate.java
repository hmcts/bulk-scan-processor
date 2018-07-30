package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusUpdate {

    public final NewStatus status;

    public StatusUpdate(
        @JsonProperty("status") NewStatus status
    ) {
        this.status = status;
    }
}
