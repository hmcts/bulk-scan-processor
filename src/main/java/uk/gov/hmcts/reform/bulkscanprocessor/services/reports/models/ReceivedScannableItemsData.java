package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceivedScannableItemsData {
    @JsonProperty("container")
    private final String container;

    @JsonProperty("count")
    private final int count;

    public ReceivedScannableItemsData(
            String container,
            int count
    ) {
        this.container = container;
        this.count = count;
    }
}
