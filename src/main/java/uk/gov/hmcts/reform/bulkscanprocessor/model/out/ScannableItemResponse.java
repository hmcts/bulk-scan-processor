package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampSerialiser;

import java.sql.Timestamp;

public class ScannableItemResponse {

    @JsonProperty("document_control_number")
    private final String documentControlNumber;

    @JsonProperty("scanning_date")
    @JsonSerialize(using = CustomTimestampSerialiser.class)
    private final Timestamp scanningDate;

    @JsonProperty("ocr_accuracy")
    private final String ocrAccuracy;

    @JsonProperty("manual_intervention")
    private final String manualIntervention;

    @JsonProperty("next_action")
    private final String nextAction;

    @JsonSerialize(using = CustomTimestampSerialiser.class)
    @JsonProperty("next_action_date")
    private final Timestamp nextActionDate;

    @JsonProperty("ocr_data")
    private final String ocrData;

    @JsonProperty("file_name")
    private final String fileName;

    @JsonProperty("notes")
    private final String notes;

    @JsonProperty("document_url")
    private String documentUrl;

    @JsonProperty("document_type")
    private final String documentType;

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
        @JsonProperty("ocr_data") String ocrData,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("notes") String notes,
        @JsonProperty("document_type") String documentType
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
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public Timestamp getScanningDate() {
        return scanningDate;
    }

    public String getOcrAccuracy() {
        return ocrAccuracy;
    }

    public String getManualIntervention() {
        return manualIntervention;
    }

    public String getNextAction() {
        return nextAction;
    }

    public Timestamp getNextActionDate() {
        return nextActionDate;
    }

    public String getOcrData() {
        return ocrData;
    }

    public String getNotes() {
        return notes;
    }

    public String getDocumentType() {
        return documentType;
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
            + '}';
    }
    
}
