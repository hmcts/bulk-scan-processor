package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Represents a non-scannable item in an envelope.
 */
@Entity
@Table(name = "non_scannable_items")
public class NonScannableItem implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String documentControlNumber;

    private String itemType;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    /**
     * Default constructor for hibernate.
     */
    private NonScannableItem() {
        // For use by hibernate.
    }

    /**
     * Creates a new non-scannable item.
     * @param documentControlNumber document control number
     * @param itemType type of the item
     * @param notes notes
     */
    public NonScannableItem(
        String documentControlNumber,
        String itemType,
        String notes
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
        this.notes = notes;
    }

    /**
     * Get document control number.
     */
    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    /**
     * Get item type.
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * Get notes.
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Set envelope.
     * @param envelope envelope
     */
    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
