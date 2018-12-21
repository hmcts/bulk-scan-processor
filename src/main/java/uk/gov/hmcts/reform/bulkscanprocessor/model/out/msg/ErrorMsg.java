package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message describing an error processing an envelope.
 */
public class ErrorMsg implements Msg {

    public final String id;
    public final Long eventId;
    public final String zipFileName;
    public final String jurisdiction;
    public final String poBox;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;

    // region constructors
    @SuppressWarnings("squid:S00107") // number of params
    public ErrorMsg(
        @JsonProperty(value = "id", required = true) String id,
        @JsonProperty(value = "eventId", required = true) Long eventId,
        @JsonProperty(value = "zipFileName", required = true) String zipFileName,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonProperty("poBox") String poBox,
        @JsonProperty("documentControlNumber") String documentControlNumber,
        @JsonProperty(value = "errorCode", required = true) ErrorCode errorCode,
        @JsonProperty(value = "errorDescription", required = true) String errorDescription
    ) {
        this.id = id;
        this.eventId = eventId;
        this.zipFileName = zipFileName;
        this.jurisdiction = jurisdiction;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
    // endregion

    // region getters
    @JsonIgnore
    @Override
    public String getMsgId() {
        return this.id;
    }

    @JsonIgnore
    @Override
    public boolean isTestOnly() {
        return false;
    }
    // endregion
}
