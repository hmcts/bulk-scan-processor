package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the data for received scannable items per document type.
 */
public class ReceivedScannableItemsPerDocumentTypeData extends ReceivedScannableItemsData {
    @JsonProperty("document_type")
    private final String documentType;

    /**
     * Constructor for the ReceivedScannableItemsPerDocumentTypeData.
     * @param container The container
     * @param documentType The document type
     * @param count The count
     */
    public ReceivedScannableItemsPerDocumentTypeData(String container, String documentType, int count) {
        super(container, count);
        this.documentType = documentType;
    }

    /**
     * Returns the document type.
     * @return The document type
     */
    public String getDocumentType() {
        return documentType;
    }
}
