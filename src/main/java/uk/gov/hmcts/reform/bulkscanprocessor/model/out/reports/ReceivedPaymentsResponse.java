package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedPaymentsData;

import java.util.List;

/**
 * Response for received payments report.
 */
public class ReceivedPaymentsResponse {

    @JsonProperty("total")
    public final int total;

    @JsonProperty("received_payments")
    public final List<ReceivedPaymentsData> receivedPayments;

    /**
     * Constructor for ReceivedPaymentsResponse.
     * @param total total number of received payments
     * @param receivedPayments list of received payments
     */
    public ReceivedPaymentsResponse(int total, List<ReceivedPaymentsData> receivedPayments) {
        this.total = total;
        this.receivedPayments = receivedPayments;
    }
}
