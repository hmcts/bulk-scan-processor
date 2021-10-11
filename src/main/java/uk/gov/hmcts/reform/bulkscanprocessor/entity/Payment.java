package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.time.Instant;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String documentControlNumber;

    private String status;

    private Instant lastmodified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    private Payment() {
        // For use by hibernate.
    }

    public Payment(UUID id, String documentControlNumber, String status, Instant lastmodified) {
        this.id = id;
        this.status = status;
        this.documentControlNumber = documentControlNumber;
        this.lastmodified = lastmodified;
    }

    public Payment(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastmodified() {
        return lastmodified;
    }
}
