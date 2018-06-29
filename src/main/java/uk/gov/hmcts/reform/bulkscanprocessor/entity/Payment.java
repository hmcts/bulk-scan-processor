package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @JsonProperty("document_control_number")
    private String documentControlNumber;
    @JsonProperty("method")
    private String method;
    @JsonProperty("amount_in_pence")
    private int amount;
    @JsonProperty("currency")
    private String currency;

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
        this.amount = amountInPence.intValue();
        this.currency = currency;
    }

    @JsonProperty("amount")
    public double getAmount() {
        return ((double) amount) / 100;
    }
}
