package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResponse {

    @NotNull
    private List<Document> documents;

    @JsonCreator
    public UploadResponse(@JsonProperty("documents") List<Document> documents) {
        this.documents = documents;
    }

    public List<Document> getDocuments() {
        return documents;
    }
}
