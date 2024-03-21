package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the response from the document management store.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResponse {

    private List<Document> documents;

    /**
     * Constructor for UploadResponse.
     * @param documents The documents
     */
    @JsonCreator
    public UploadResponse(@JsonProperty("documents") List<Document> documents) {
        this.documents = documents;
    }

    /**
     * Get the documents.
     * @return The documents
     */
    public List<Document> getDocuments() {
        return documents;
    }
}
