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
    private String documentControlNumber;

    @JsonProperty("scanning_date")
    @JsonSerialize(using = CustomTimestampSerialiser.class)
    private Timestamp scanningDate;

    @JsonProperty("ocr_accuracy")
    private String ocrAccuracy;

    @JsonProperty("manual_intervention")
    private String manualIntervention;

    @JsonProperty("next_action")
    private String nextAction;

    @JsonSerialize(using = CustomTimestampSerialiser.class)
    @JsonProperty("next_action_date")
    private Timestamp nextActionDate;

    @JsonProperty("ocr_data")
    private String ocrData;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("document_url")
    private String documentUrl;

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
        @JsonProperty("notes") String notes
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

    @Override
    public String toString() {
        return "ScannableItemResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + ", scanningDate=" + scanningDate
            + ", ocrAccuracy='" + ocrAccuracy + '\''
            + ", manualIntervention='" + manualIntervention + '\''
            + ", nextAction='" + nextAction + '\''
            + ", nextActionDate=" + nextActionDate
            + ", ocrData='" + ocrData + '\''
            + ", fileName='" + fileName + '\''
            + ", notes='" + notes + '\''
            + ", documentUrl='" + documentUrl + '\''
            + '}';
    }
    
}
