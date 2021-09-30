package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedPaymentsData;

import java.util.List;

public class ReceivedPaymentsResponse {

    @JsonProperty("total")
    public final int total;

    @JsonProperty("received_payments")
    public final List<ReceivedPaymentsData> receivedPayments;

    public ReceivedPaymentsResponse(int total, List<ReceivedPaymentsData> receivedPayments) {
        this.total = total;
        this.receivedPayments = receivedPayments;
    }
}
