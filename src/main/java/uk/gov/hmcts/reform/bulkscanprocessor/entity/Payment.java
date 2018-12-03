package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import java.math.BigDecimal;
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
        String documentControlNumber,
        String method,
        BigDecimal amount,
        String currency,
        String paymentInstrumentNumber,
        String sortCode,
        String accountNumber
    ) {
        this.documentControlNumber = documentControlNumber;
        this.method = method;
        this.amountInPence = amount.multiply(BigDecimal.valueOf(100)).intValue();
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
