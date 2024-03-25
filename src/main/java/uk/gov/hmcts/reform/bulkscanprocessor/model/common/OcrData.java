package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents OCR data extracted from a document.
 */
public class OcrData {

    @JsonProperty("Metadata_file")
    public final List<OcrDataField> fields;

    /**
     * Constructor.
     * @param fields The fields
     */
    public OcrData(@JsonProperty("Metadata_file") List<OcrDataField> fields) {
        this.fields = fields;
    }
}
