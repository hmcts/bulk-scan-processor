package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentResponse {

    @JsonProperty("document_control_number")
    private final String documentControlNumber;

    @JsonProperty("method")
    private final String method;

    @JsonProperty("amount_in_pence")
    private final int amountInPence;

    @JsonProperty("currency")
    private final String currency;

    @JsonProperty("payment_instrument_number")
    private final String paymentInstrumentNumber;

    @JsonProperty("sort_code")
    private final String sortCode;

    @JsonProperty("account_number")
    private final String accountNumber;

    public PaymentResponse(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("method") String method,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("payment_instrument_number") String paymentInstrumentNumber,
        @JsonProperty("sort_code") String sortCode,
        @JsonProperty("account_number") String accountNumber
    ) {
        Double pence = Double.valueOf(amount) * 100;

        this.documentControlNumber = documentControlNumber;
        this.method = method;
        this.amountInPence = pence.intValue();
        this.currency = currency;
        this.paymentInstrumentNumber = paymentInstrumentNumber;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
    }

    @JsonProperty("amount")
    public double getAmount() {
        return ((double) amountInPence) / 100;
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

    @Override
    public String toString() {
        return "PaymentResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + ", method='" + method + '\''
            + ", amountInPence=" + amountInPence
            + ", currency='" + currency + '\''
            + ", paymentInstrumentNumber='" + paymentInstrumentNumber + '\''
            + ", sortCode='" + sortCode + '\''
            + ", accountNumber='" + accountNumber + '\''
            + '}';
    }
    
}
