package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NonScannableItemResponse {

    @JsonProperty("item_type")
    private String itemType;
    @JsonProperty("notes")
    private String notes;

    public NonScannableItemResponse(
        @JsonProperty("item_type") String itemType,
        @JsonProperty("notes") String notes
    ) {
        this.itemType = itemType;
        this.notes = notes;
    }

}
