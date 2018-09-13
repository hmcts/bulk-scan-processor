package uk.gov.hmcts.reform.bulkscanprocessor.entity;

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
    private UUID id;

    private String documentControlNumber;

    private String method;

    private int amountInPence;

    private String currency;

    private String paymentInstrumentNumber;

    private String sortCode;

    private String accountNumber;

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
        @JsonProperty("currency") String currency,
        @JsonProperty("payment_instrument_number") String paymentInstrumentNumber,
        @JsonProperty("sort_code") String sortCode,
        @JsonProperty("account_number") String accountNumber
    ) {
        Double amountInPence = Double.valueOf(amount) * 100;

        this.documentControlNumber = documentControlNumber;
        this.method = method;
        this.amountInPence = amountInPence.intValue();
        this.currency = currency;
        this.paymentInstrumentNumber = paymentInstrumentNumber;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
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

    public String getPaymentInstrumentNumber() {
        return paymentInstrumentNumber;
    }

    public String getSortCode() {
        return sortCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
