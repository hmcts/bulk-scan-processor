package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CustomTimestampDeserialiser;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "scannable_items")
public class ScannableItem implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String documentControlNumber;

    private Timestamp scanningDate;

    private String ocrAccuracy;

    private String manualIntervention;

    private String nextAction;

    private Timestamp nextActionDate;

    private String ocrData;

    private String fileName;

    private String notes;

    private String documentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    private ScannableItem() {
        // For use by hibernate.
    }

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

    public UUID getId() {
        return id;
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

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
