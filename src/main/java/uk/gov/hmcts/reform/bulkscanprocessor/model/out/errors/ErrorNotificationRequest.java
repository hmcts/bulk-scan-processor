package uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorNotificationRequest {

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("po_box")
    public final String poBox;

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("error_code")
    public final String errorCode;

    @JsonProperty("error_description")
    public final String errorDescription;

    @JsonProperty("reference_id")
    public final String referenceId;

    @JsonProperty("service")
    public final String service;

    /**
     * Full request body definition.
     *
     * @param zipFileName Name of the zip file
     * @param poBox POBox to which envelope was received
     * @param documentControlNumber Document control number
     * @param errorCode Error code
     * @param errorDescription Error message describing cause of notification
     * @param referenceId TBD
     *
     * @see uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode
     */
    public ErrorNotificationRequest(
        String zipFileName,
        String poBox,
        String documentControlNumber,
        String errorCode,
        String errorDescription,
        String referenceId,
        String service
    ) {
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.referenceId = referenceId;
        this.service = service;
    }

    /**
     * Constructor with mandatory fields.
     *
     * @param zipFileName Name of the zip file
     * @param poBox POBox to which envelope was received
     * @param errorCode Error code
     * @param errorDescription Error message describing cause of notification
     *
     * @see uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode
     */
    public ErrorNotificationRequest(
        String zipFileName,
        String poBox,
        String errorCode,
        String errorDescription,
        String service
    ) {
        this(
            zipFileName,
            poBox,
            null,
            errorCode,
            errorDescription,
            null,
            service
        );
    }
}
