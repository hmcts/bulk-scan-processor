package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NonScannableItemResponse {

    @JsonProperty("document_control_number")
    private final String documentControlNumber;

    @JsonProperty("item_type")
    private final String itemType;
    
    @JsonProperty("notes")
    private final String notes;

    public NonScannableItemResponse(
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty("item_type") String itemType,
        @JsonProperty("notes") String notes
    ) {
        this.documentControlNumber = documentControlNumber;
        this.itemType = itemType;
        this.notes = notes;
    }

    public String getDocumentControlNumber() {
        return documentControlNumber;
    }

    public String getItemType() {
        return itemType;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "NonScannableItemResponse{"
            + "documentControlNumber='" + documentControlNumber + '\''
            + "itemType='" + itemType + '\''
            + ", notes='" + notes + '\''
            + '}';
    }
    
}
