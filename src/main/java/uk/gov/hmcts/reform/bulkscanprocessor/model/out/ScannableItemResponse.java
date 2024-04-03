package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.util.InstantSerializer;

import java.time.Instant;

/**
 * Represents the response for a scannable item.
 */
public class ScannableItemResponse {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("scanning_date")
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant scanningDate;

    @JsonProperty("ocr_accuracy")
    public final String ocrAccuracy;

    @JsonProperty("manual_intervention")
    public final String manualIntervention;

    @JsonProperty("next_action")
    public final String nextAction;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("next_action_date")
    public final Instant nextActionDate;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("document_uuid")
    public final String documentUuid;

    @JsonProperty("document_type")
    public final DocumentType documentType;

    @JsonProperty("document_subtype")
    public final String documentSubtype;

    @JsonProperty("has_ocr_data")
    public final boolean hasOcrData;

    /**
     * Constructor for ScannableItemResponse.
     * @param documentControlNumber Document control number
     * @param scanningDate Scanning date
     * @param ocrAccuracy OCR accuracy
     * @param manualIntervention Manual intervention
     * @param nextAction Next action
     * @param nextActionDate Next action date
     * @param fileName File name
     * @param documentUuid Document UUID
     * @param documentType Document type
     * @param documentSubtype Document subtype
     * @param hasOcrData Has OCR data
     */
    @JsonCreator
    public ScannableItemResponse(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("scanning_date") Instant scanningDate,
        @JsonProperty("ocr_accuracy") String ocrAccuracy,
        @JsonProperty("manual_intervention") String manualIntervention,
        @JsonProperty("next_action") String nextAction,
        @JsonDeserialize(using = InstantDeserializer.class)
        @JsonProperty("next_action_date") Instant nextActionDate,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("document_uuid") String documentUuid,
        @JsonProperty("document_type") DocumentType documentType,
        @JsonProperty("document_subtype") String documentSubtype,
        @JsonProperty("has_ocr_data") boolean hasOcrData
    ) {
        this.documentControlNumber = documentControlNumber;
        this.scanningDate = scanningDate;
        this.ocrAccuracy = ocrAccuracy;
        this.manualIntervention = manualIntervention;
        this.nextAction = nextAction;
        this.nextActionDate = nextActionDate;
        this.fileName = fileName;
        this.documentUuid = documentUuid;
        this.documentType = documentType;
        this.documentSubtype = documentSubtype;
        this.hasOcrData = hasOcrData;
    }

    /**
     * Returns the string representation of the object.
     * @return String representation of the object
     */
    @Override
    public String toString() {
        return "ScannableItemResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + ", scanningDate=" + scanningDate
            + ", ocrAccuracy='" + ocrAccuracy + '\''
            + ", manualIntervention='" + manualIntervention + '\''
            + ", nextAction='" + nextAction + '\''
            + ", nextActionDate=" + nextActionDate
            + ", fileName='" + fileName + '\''
            + ", documentUuid='" + documentUuid + '\''
            + ", documentType='" + documentType + '\''
            + ", documentSubtype='" + documentSubtype + '\''
            + '}';
    }

}
