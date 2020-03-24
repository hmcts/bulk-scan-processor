package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message describing an error processing an envelope.
 */
public class ErrorMsg implements Msg {

    public final String id;
    public final Long eventId;

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("jurisdiction")
    public final String jurisdiction;

    @JsonProperty("po_box")
    public final String poBox;

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("error_code")
    public final ErrorCode errorCode;

    @JsonProperty("error_description")
    public final String errorDescription;
    public final String service;

    // region constructors
    @SuppressWarnings("squid:S00107") // number of params
    public ErrorMsg(
        // TODO: make id transient when the application stops sending error notifications
        @JsonProperty(value = "id", required = true)
        String id,
        // TODO: remove eventId when the application stops sending error notifications
        @JsonProperty(value = "eventId", required = true) Long eventId,
        @JsonProperty(value = "zip_file_name", required = true) String zipFileName,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonProperty("po_box") String poBox,
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty(value = "error_code", required = true) ErrorCode errorCode,
        @JsonProperty(value = "error_description", required = true) String errorDescription,
        @JsonProperty(value = "service", required = true) String service
    ) {
        this.id = id;
        this.eventId = eventId;
        this.zipFileName = zipFileName;
        this.jurisdiction = jurisdiction;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.service = service;
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
    public String getLabel() {
        return null;
    }
    // endregion
}
