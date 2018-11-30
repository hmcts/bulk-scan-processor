package uk.gov.hmcts.reform.bulkscanprocessor.entity;

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
public class DbScannableItem implements EnvelopeAssignable {

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

    private String documentType = "Other";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private DbEnvelope envelope;

    private DbScannableItem() {
        // For use by hibernate.
    }

    public DbScannableItem(
        String documentControlNumber,
        Timestamp scanningDate,
        String ocrAccuracy,
        String manualIntervention,
        String nextAction,
        Timestamp nextActionDate,
        String ocrData,
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
    public void setEnvelope(DbEnvelope envelope) {
        this.envelope = envelope;
    }
}
