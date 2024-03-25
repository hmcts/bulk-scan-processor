package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a non-scannable item.
 */
public class NonScannableItemResponse {

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("item_type")
    public final String itemType;

    /**
     * Constructor for NonScannableItemResponse.
     * @param documentControlNumber Document control number
     * @param itemType Item type
     */
    public NonScannableItemResponse(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("item_type") String itemType
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
    }

    /**
     * Returns the string representation of the object.
     * @return String representation of the object
     */
    @Override
    public String toString() {
        return "NonScannableItemResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + ", itemType='" + itemType + '\''
            + '}';
    }
}
