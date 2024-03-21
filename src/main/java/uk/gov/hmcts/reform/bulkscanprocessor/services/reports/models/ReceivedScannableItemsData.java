package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the data for received scannable items.
 */
public class ReceivedScannableItemsData {
    @JsonProperty("container")
    private final String container;

    @JsonProperty("count")
    private final int count;

    /**
     * Constructor for the ReceivedScannableItemsData.
     * @param container The container
     * @param count The count
     */
    public ReceivedScannableItemsData(
            String container,
            int count
    ) {
        this.container = container;
        this.count = count;
    }
}
