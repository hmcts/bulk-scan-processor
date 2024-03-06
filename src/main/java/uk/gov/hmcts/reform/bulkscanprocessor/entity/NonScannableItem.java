package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    private NonScannableItem() {
        // For use by hibernate.
    }

    public NonScannableItem(
        String documentControlNumber,
        String itemType,
        String notes
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
        this.notes = notes;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public String getItemType() {
        return itemType;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
