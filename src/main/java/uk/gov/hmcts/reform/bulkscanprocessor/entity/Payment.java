package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonIgnore
    private UUID id;

    @JsonProperty("document_control_number")
    private String documentControlNumber;
    @JsonProperty("method")
    private String method;
    @JsonProperty("amount_in_pence")
    private int amountInPence;
    @JsonProperty("currency")
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", nullable = false)
    private Envelope envelope;

    private Payment() {
        // For use by hibernate.
    }

    public Payment(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("method") String method,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency
    ) {
        Double amountInPence = Double.valueOf(amount) * 100;

        this.documentControlNumber = documentControlNumber;
        this.method = method;
        this.amountInPence = amountInPence.intValue();
        this.currency = currency;
    }

    @JsonProperty("amount")
    public double getAmount() {
        return ((double) amountInPence) / 100;
    }

    @Override
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
