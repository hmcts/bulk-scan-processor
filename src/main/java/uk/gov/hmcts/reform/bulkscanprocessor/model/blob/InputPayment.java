package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class InputPayment {

    public final String documentControlNumber;
    public final String method;
    public final BigDecimal amount;
    public final String currency;
    public final String paymentInstrumentNumber;
    public final String sortCode;
    public final String accountNumber;

    public InputPayment(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("method") String method,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("payment_instrument_number") String paymentInstrumentNumber,
        @JsonProperty("sort_code") String sortCode,
        @JsonProperty("account_number") String accountNumber
    ) {
        this.documentControlNumber = documentControlNumber;
        this.method = method;
        this.amount = amount == null ? BigDecimal.ZERO : new BigDecimal(amount);
        this.currency = currency;
        this.paymentInstrumentNumber = paymentInstrumentNumber;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
    }
}
