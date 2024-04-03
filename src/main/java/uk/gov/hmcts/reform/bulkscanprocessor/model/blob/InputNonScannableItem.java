package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a non-scannable item in a document.
 */
public class InputNonScannableItem {

    public final String documentControlNumber;
    public final String itemType;
    public final String notes;

    /**
     * Constructor for InputNonScannableItem.
     * @param documentControlNumber the document control number
     * @param itemType the type of the item
     * @param notes the notes
     */
    public InputNonScannableItem(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("item_type") String itemType,
        @JsonProperty("notes") String notes
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
        this.notes = notes;
    }
}
