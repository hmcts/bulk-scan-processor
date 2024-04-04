package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a payment in an envelope.
 */
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

    /**
     * Default constructor for hibernate.
     */
    private Payment() {
        // For use by hibernate.
    }

    /**
     * Creates a new payment.
     * @param id payment id
     * @param documentControlNumber document control number
     * @param status payment status
     * @param lastmodified last modified date
     */
    public Payment(UUID id, String documentControlNumber, String status, Instant lastmodified) {
        this.id = id;
        this.status = status;
        this.documentControlNumber = documentControlNumber;
        this.lastmodified = lastmodified;
    }

    /**
     * Creates a new payment.
     * @param documentControlNumber document control number
     */
    public Payment(String documentControlNumber) {
        this.documentControlNumber = documentControlNumber;
    }

    /**
     * Sets the envelope for this payment.
     * @param envelope envelope
     */
    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    /**
     * Get document control number.
     */
    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    /**
     * Get payment id.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get payment status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get last modified date.
     */
    public Instant getLastmodified() {
        return lastmodified;
    }
}
