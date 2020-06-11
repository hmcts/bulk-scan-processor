package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "scannable_items")
@TypeDefs({
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class),
    @TypeDef(name = "string-array", typeClass = StringArrayType.class)
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

    @Type(type = "jsonb")
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

    @Type(type = "string-array")
    @Column(name = "ocrValidationWarnings", columnDefinition = "varchar[]")
    private String[] ocrValidationWarnings;

    private int documentOrder;

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
        String[] ocrValidationWarnings,
        int documentOrder
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
        this.documentOrder = documentOrder;
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

    @Override
    public String toString() {
        return "ScannableItem{"
            + "id=" + id
            + ", documentControlNumber='" + documentControlNumber + '\''
            + ", scanningDate=" + scanningDate
            + ", ocrAccuracy='" + ocrAccuracy + '\''
            + ", manualIntervention='" + manualIntervention + '\''
            + ", nextAction='" + nextAction + '\''
            + ", nextActionDate=" + nextActionDate
            + ", ocrData=" + ocrData
            + ", fileName='" + fileName + '\''
            + ", notes='" + notes + '\''
            + ", documentUuid='" + documentUuid + '\''
            + ", documentType=" + documentType
            + ", documentSubtype='" + documentSubtype + '\''
            + ", envelope=" + envelope
            + ", ocrValidationWarnings=" + Arrays.toString(ocrValidationWarnings)
            + '}';
    }

    public int getDocumentOrder() {
        return documentOrder;
    }

    public void setDocumentOrder(int documentOrder) {
        this.documentOrder = documentOrder;
    }
}
