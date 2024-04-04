package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;

import java.time.Instant;
import java.util.UUID;


/**
 * Represents a scannable item.
 */
@Entity
@Table(name = "scannable_items")
public class ScannableItem implements EnvelopeAssignable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String documentControlNumber;

    private Instant scanningDate;

    private String ocrAccuracy;

    private String manualIntervention;

    private String nextAction;

    private Instant nextActionDate;

    @JdbcTypeCode(SqlTypes.JSON)
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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ocrValidationWarnings", columnDefinition = "varchar[]")
    private String[] ocrValidationWarnings;

    /**
     * Default constructor.
     */
    private ScannableItem() {
        // For use by hibernate.
    }

    /**
     * Create a new instance of ScannableItem.
     * @param documentControlNumber document control number
     * @param scanningDate scanning date
     * @param ocrAccuracy ocr accuracy
     * @param manualIntervention manual intervention
     * @param nextAction next action
     * @param nextActionDate next action date
     * @param ocrData ocr data
     * @param fileName file name
     * @param notes notes
     * @param documentType document type
     * @param documentSubtype document subtype
     * @param ocrValidationWarnings ocr validation warnings
     */
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

    /**
     * Get the id.
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the file name.
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get document control number.
     * @return the document control number
     */
    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    /**
     * Get the scanning date.
     * @return the scanning date
     */
    public Instant getScanningDate() {
        return scanningDate;
    }

    /**
     * Get the ocr accuracy.
     * @return the ocr accuracy
     */
    public String getOcrAccuracy() {
        return ocrAccuracy;
    }

    /**
     * Get the manual intervention.
     * @return the manual intervention
     */
    public String getManualIntervention() {
        return manualIntervention;
    }

    /**
     * Get the next action.
     * @return the next action
     */
    public String getNextAction() {
        return nextAction;
    }

    /**
     * Get the next action date.
     * @return the next action date
     */
    public Instant getNextActionDate() {
        return nextActionDate;
    }

    /**
     * Get the ocr data.
     * @return the ocr data
     */
    public OcrData getOcrData() {
        return ocrData;
    }

    /**
     * Set the ocr data.
     * @param ocrData the ocr data
     */
    public void setOcrData(OcrData ocrData) {
        this.ocrData = ocrData;
    }

    /**
     * Get the notes.
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Get the document type.
     * @return the document type
     */
    public DocumentType getDocumentType() {
        return documentType;
    }

    /**
     * Get the document subtype.
     * @return the document subtype
     */
    public String getDocumentSubtype() {
        return documentSubtype;
    }

    /**
     * Get the document uuid.
     * @return the document uuid
     */
    public String getDocumentUuid() {
        return documentUuid;
    }

    /**
     * Set the document uuid.
     * @param documentUuid the document uuid
     */
    public void setDocumentUuid(String documentUuid) {
        this.documentUuid = documentUuid;
    }

    /**
     * Set the envelope.
     * @param envelope the envelope
     */
    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    /**
     * Get ocr validation warnings.
     * @return the ocr validation warnings
     */
    public String[] getOcrValidationWarnings() {
        return ocrValidationWarnings;
    }

    /**
     * Set ocr validation warnings.
     * @param ocrValidationWarnings the ocr validation warnings
     */
    public void setOcrValidationWarnings(String[] ocrValidationWarnings) {
        this.ocrValidationWarnings = ocrValidationWarnings;
    }
}
