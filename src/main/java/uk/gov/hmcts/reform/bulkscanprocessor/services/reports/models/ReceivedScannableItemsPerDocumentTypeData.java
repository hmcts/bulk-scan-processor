package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReceivedScannableItemsPerDocumentTypeData extends ReceivedScannableItemsData {
    @JsonProperty("document_type")
    private final String documentType;

    public ReceivedScannableItemsPerDocumentTypeData(String container, String documentType, int count) {
        super(container, count);
        this.documentType = documentType;
    }

    public String getDocumentType() {
        return documentType;
    }
}
