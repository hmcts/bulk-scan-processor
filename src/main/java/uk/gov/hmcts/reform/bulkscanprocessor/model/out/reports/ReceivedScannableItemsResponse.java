package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedScannableItemsData;

import java.util.List;

public class ReceivedScannableItemsResponse {

    @JsonProperty("total")
    public final int total;

    @JsonProperty("received_scannable_items")
    public final List<ReceivedScannableItemsData> receivedScannableItems;

    public ReceivedScannableItemsResponse(int total, List<ReceivedScannableItemsData> receivedScannableItems) {
        this.total = total;
        this.receivedScannableItems = receivedScannableItems;
    }
}
