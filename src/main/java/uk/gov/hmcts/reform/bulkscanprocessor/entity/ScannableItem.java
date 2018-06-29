package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "scannable_items")
public class ScannableItem {

    @Id
    private UUID id;

    @JsonProperty("document_control_number")
    private String documentControlNumber;
    @JsonProperty("scanning_date")
    private Timestamp scanningDate;
    @JsonProperty("ocr_accuracy")
    private String ocrAccuracy;
    @JsonProperty("manual_intervention")
    private String manualIntervention;
    @JsonProperty("next_action")
    private String nextAction;
    @JsonProperty("next_action_date")
    private Timestamp nextActionDate;
    @JsonProperty("ocr_data")
    private String ocrData;
    @JsonProperty("file_name")
    private String fileName;
    @JsonProperty("notes")
    private String notes;

    private ScannableItem() {
        // For use by hibernate.
    }

    public ScannableItem(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss.SSSSSS")
        @JsonProperty("scanning_date") Timestamp scanningDate,
        @JsonProperty("ocr_accuracy") String ocrAccuracy,
        @JsonProperty("manual_intervention") String manualIntervention,
        @JsonProperty("next_action") String nextAction,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss.SSSSSS")
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
}
