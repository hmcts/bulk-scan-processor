package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedScannableItemsData;

import java.util.List;

/**
 * Response for received scannable items report.
 */
public class ReceivedScannableItemsResponse {

    @JsonProperty("total")
    public final int total;

    @JsonProperty("received_scannable_items")
    public final List<ReceivedScannableItemsData> receivedScannableItems;

    /**
     * Constructor for ReceivedScannableItemsResponse.
     * @param total total number of received scannable items
     * @param receivedScannableItems list of received scannable items
     */
    public ReceivedScannableItemsResponse(int total, List<ReceivedScannableItemsData> receivedScannableItems) {
        this.total = total;
        this.receivedScannableItems = receivedScannableItems;
    }
}
