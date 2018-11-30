package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "non_scannable_items")
public class DbNonScannableItem implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String documentControlNumber;

    private String itemType;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private DbEnvelope envelope;

    private DbNonScannableItem() {
        // For use by hibernate.
    }

    public DbNonScannableItem(
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
    public void setEnvelope(DbEnvelope envelope) {
        this.envelope = envelope;
    }
}
