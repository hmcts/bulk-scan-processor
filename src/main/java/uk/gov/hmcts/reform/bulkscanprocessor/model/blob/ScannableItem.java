package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;

import java.sql.Timestamp;

public class ScannableItem {

    public final String documentControlNumber;
    public final Timestamp scanningDate;
    public final String ocrAccuracy;
    public final String manualIntervention;
    public final String nextAction;
    public final Timestamp nextActionDate;
    public final String ocrData;
    public final String fileName;
    public final String notes;
    public final String documentType;

    @JsonCreator
    public ScannableItem(
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
}
