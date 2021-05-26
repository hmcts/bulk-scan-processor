package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NonScannableItemResponse {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("item_type")
    public final String itemType;

    public NonScannableItemResponse(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("item_type") String itemType
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
    }

    @Override
    public String toString() {
        return "NonScannableItemResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + ", itemType='" + itemType + '\''
            + '}';
    }
}
