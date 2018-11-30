package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NonScannableItem {

    public final String documentControlNumber;
    public final String itemType;
    public final String notes;

    public NonScannableItem(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("item_type") String itemType,
        @JsonProperty("notes") String notes
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
        this.notes = notes;
    }
}
