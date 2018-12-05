package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "scannable_items")
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
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

    @Type(type = "jsonb")
    @Column(name = "ocrDataJson", columnDefinition = "jsonb")
    private Map<String, String> ocrData;

    private String fileName;

    private String notes;

    private String documentUrl;

    private String documentType = "Other";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    private ScannableItem() {
        // For use by hibernate.
    }

    public ScannableItem(
        String documentControlNumber,
        Timestamp scanningDate,
        String ocrAccuracy,
        String manualIntervention,
        String nextAction,
        Timestamp nextActionDate,
        Map<String, String> ocrData,
        String fileName,
        String notes,
        String documentType
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

    public Map<String, String> getOcrData() {
        return ocrData;
    }

    public String getNotes() {
        return notes;
    }

    public String getDocumentType() {
        return documentType;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
