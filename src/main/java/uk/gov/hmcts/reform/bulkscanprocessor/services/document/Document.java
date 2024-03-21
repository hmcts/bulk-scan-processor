package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.ccd.document.am.model.Classification;

import java.util.Map;

/**
 * Represents a document in CCD.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {

    public final Classification classification;

    public final long size;

    public final String mimeType;

    public final String originalDocumentName;

    public final String hashToken;

    public final Map<String, String> metadata;

    public final Links links;

    /**
     * Constructor for Document.
     * @param classification The classification of the document
     * @param size The size of the document
     * @param mimeType The mime type of the document
     * @param originalDocumentName The original document name
     * @param hashToken The hash token of the document
     * @param metadata The metadata of the document
     * @param links The links of the document
     */
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


    /**
     * Represents the links of a document.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Links {
        public final Link self;
        public final Link binary;

        /**
         * Constructor for Links.
         * @param self The self link
         * @param binary The binary link
         */
        public Links(@JsonProperty("self") Link self, @JsonProperty("binary") Link binary) {
            this.self = self;
            this.binary = binary;
        }
    }

    /**
     * Represents a link.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        public final String href;

        /**
         * Constructor for Link.
         * @param href The href of the link
         */
        public Link(@JsonProperty("href") String href) {
            this.href = href;
        }
    }
}
