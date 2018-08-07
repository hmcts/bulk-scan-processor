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
@Table(name = "payments")
public class Payment implements EnvelopeAssignable {

    @Id
    @GeneratedValue
    private UUID id;

    private String documentControlNumber;

    private String method;

    private int amountInPence;

    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    private Payment() {
        // For use by hibernate.
    }

    public Payment(
        String documentControlNumber,
        String method,
        String amount,
        String currency
    ) {
        Double amountInPence = Double.valueOf(amount) * 100;

        this.documentControlNumber = documentControlNumber;
        this.method = method;
        this.amountInPence = amountInPence.intValue();
        this.currency = currency;
    }

    public double getAmount() {
        return ((double) amountInPence) / 100;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public String getMethod() {
        return method;
    }

    public int getAmountInPence() {
        return amountInPence;
    }

    public String getCurrency() {
        return currency;
    }

}
