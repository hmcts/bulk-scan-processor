package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentResponse {

    @JsonProperty("document_control_number")
    private String documentControlNumber;

    @JsonProperty("method")
    private String method;

    @JsonProperty("amount_in_pence")
    private int amountInPence;

    @JsonProperty("currency")
    private String currency;

    public PaymentResponse(
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

    @Override
    public String toString() {
        return "PaymentResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + ", method='" + method + '\''
            + ", amountInPence=" + amountInPence
            + ", currency='" + currency + '\''
            + '}';
    }
    
}
