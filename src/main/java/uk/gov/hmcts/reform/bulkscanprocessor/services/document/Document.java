package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {

    public final Classification classification;

    public final long size;

    public final String mimeType;

    public final String originalDocumentName;

    public final String hashToken;

    public final Map<String, String> metadata;

    public final Links links;

    public Document(
        @JsonProperty("classification") Classification classification,
        @JsonProperty("size") long size,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("originalDocumentName") String originalDocumentName,
        @JsonProperty("hashToken") String hashToken,
        @JsonProperty("metadata") Map<String, String> metadata,
        @JsonProperty("_links") Links links
    ) {
        this.classification = classification;
        this.size = size;
        this.mimeType = mimeType;
        this.originalDocumentName = originalDocumentName;
        this.hashToken = hashToken;
        this.metadata = metadata;
        this.links = links;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        public final Link self;
        public final Link binary;

        public Links(@JsonProperty("self") Link self, @JsonProperty("binary") Link binary) {
            this.self = self;
            this.binary = binary;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        public final String href;

        public Link(@JsonProperty("href") String href) {
            this.href = href;
        }
    }
}
