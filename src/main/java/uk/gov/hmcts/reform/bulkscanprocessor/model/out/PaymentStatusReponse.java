package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

/**
 * Represents the response of the payment status API.
 */
public class PaymentStatusReponse {
    public final String status;

    /**
     * Constructor for PaymentStatusReponse.
     * @param status Status of the payment
     */
    public PaymentStatusReponse(String status) {
        this.status = status;
    }
}
