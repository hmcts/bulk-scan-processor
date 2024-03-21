package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

/**
 * Represents a request to process payments.
 */
public class PaymentRequest {

    @NotEmpty(message = "Payment list can't be empty")
    private List<@Valid PaymentInfo> payments;

    /**
     * Constructor.
     */
    private PaymentRequest() {
    }

    /**
     * Constructor.
     * @param payments The payments
     */
    public PaymentRequest(List<PaymentInfo> payments) {
        this.payments = payments;
    }

    /**
     * Returns the payments.
     * @return The payments
     */
    public List<PaymentInfo> getPayments() {
        return payments;
    }

    /**
     * Sets the payments.
     * @param payments The payments
     */
    public void setPayments(List<PaymentInfo> payments) {
        this.payments = payments;
    }
}
