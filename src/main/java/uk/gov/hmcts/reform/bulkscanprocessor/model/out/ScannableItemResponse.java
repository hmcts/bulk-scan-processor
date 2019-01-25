package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampSerialiser;

import java.sql.Timestamp;

public class ScannableItemResponse {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("scanning_date")
    @JsonSerialize(using = CustomTimestampSerialiser.class)
    public final Timestamp scanningDate;

    @JsonProperty("ocr_accuracy")
    public final String ocrAccuracy;

    @JsonProperty("manual_intervention")
    public final String manualIntervention;

    @JsonProperty("next_action")
    public final String nextAction;

    @JsonSerialize(using = CustomTimestampSerialiser.class)
    @JsonProperty("next_action_date")
    public final Timestamp nextActionDate;

    @JsonProperty("ocr_data")
    public final OcrData ocrData;

    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("notes")
    public final String notes;

    @JsonProperty("document_url")
    public final String documentUrl;

    @JsonProperty("document_type")
    public final DocumentType documentType;

    @JsonProperty("document_subtype")
    public final DocumentSubtype documentSubtype;

    @JsonCreator
    public ScannableItemResponse(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("scanning_date") Timestamp scanningDate,
        @JsonProperty("ocr_accuracy") String ocrAccuracy,
        @JsonProperty("manual_intervention") String manualIntervention,
        @JsonProperty("next_action") String nextAction,
        @JsonDeserialize(using = CustomTimestampDeserialiser.class)
        @JsonProperty("next_action_date") Timestamp nextActionDate,
        @JsonProperty("ocr_data") OcrData ocrData,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("notes") String notes,
        @JsonProperty("document_url") String documentUrl,
        @JsonProperty("document_type") DocumentType documentType,
        @JsonProperty("document_subtype") DocumentSubtype documentSubtype
    ) {
        this.documentControlNumber = documentControlNumber;
        this.scanningDate = scanningDate;
        this.ocrAccuracy = ocrAccuracy;
        this.manualIntervention = manualIntervention;
        this.nextAction = nextAction;
        this.nextActionDate = nextActionDate;
        this.ocrData = ocrData;
        this.fileName = fileName;
        this.notes = notes;
        this.documentUrl = documentUrl;
        this.documentType = documentType;
        this.documentSubtype = documentSubtype;
    }

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
            + ", notes='" + notes + '\''
            + ", documentUrl='" + documentUrl + '\''
            + ", documentType='" + documentType + '\''
            + ", documentSubtype='" + documentSubtype + '\''
            + '}';
    }

}
