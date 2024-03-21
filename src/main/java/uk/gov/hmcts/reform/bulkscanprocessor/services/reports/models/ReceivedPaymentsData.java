package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the data for received payments.
 */
public class ReceivedPaymentsData {
    @JsonProperty("container")
    private final String container;

    @JsonProperty("count")
    private final int count;

    /**
     * Constructor for the ReceivedPaymentsData.
     * @param container The container
     * @param count The count
     */
    public ReceivedPaymentsData(
            String container,
            int count
    ) {
        this.container = container;
        this.count = count;
    }
}
