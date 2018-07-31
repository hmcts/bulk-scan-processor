package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;

public class StatusUpdate {

    public final Status status;

    public StatusUpdate(
        @JsonProperty("status") Status status
    ) {
        this.status = status;
    }
}
