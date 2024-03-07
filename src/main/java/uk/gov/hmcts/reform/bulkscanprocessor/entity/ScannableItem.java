package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "scannable_items")
@Converts({
    @Convert(attributeName = "jsonb", converter = JsonBinaryType.class),
    @Convert(attributeName = "string-array", converter = StringArrayType.class)
})
public class ScannableItem implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String documentControlNumber;

    private Instant scanningDate;

    private String ocrAccuracy;

    private String manualIntervention;

    private String nextAction;

    private Instant nextActionDate;

    @Convert(attributeName = "jsonb", converter = JsonBinaryType.class)
    @Column(name = "ocrData", columnDefinition = "jsonb")
    private OcrData ocrData;

    private String fileName;

    private String notes;

    private String documentUuid;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String documentSubtype;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    @Convert(attributeName = "string-array", converter = StringArrayType.class)
    @Column(name = "ocrValidationWarnings", columnDefinition = "varchar[]")
    private String[] ocrValidationWarnings;

    private ScannableItem() {
        // For use by hibernate.
    }

    public ScannableItem(
        String documentControlNumber,
        Instant scanningDate,
        String ocrAccuracy,
        String manualIntervention,
        String nextAction,
        Instant nextActionDate,
        OcrData ocrData,
        String fileName,
        String notes,
        DocumentType documentType,
        String documentSubtype,
        String[] ocrValidationWarnings
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
        this.documentSubtype = documentSubtype;
        this.ocrValidationWarnings = ocrValidationWarnings;
    }

    public UUID getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public Instant getScanningDate() {
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

    public Instant getNextActionDate() {
        return nextActionDate;
    }

    public OcrData getOcrData() {
        return ocrData;
    }

    public void setOcrData(OcrData ocrData) {
        this.ocrData = ocrData;
    }

    public String getNotes() {
        return notes;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentSubtype() {
        return documentSubtype;
    }

    public String getDocumentUuid() {
        return documentUuid;
    }

    public void setDocumentUuid(String documentUuid) {
        this.documentUuid = documentUuid;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public String[] getOcrValidationWarnings() {
        return ocrValidationWarnings;
    }

    public void setOcrValidationWarnings(String[] ocrValidationWarnings) {
        this.ocrValidationWarnings = ocrValidationWarnings;
    }
}
