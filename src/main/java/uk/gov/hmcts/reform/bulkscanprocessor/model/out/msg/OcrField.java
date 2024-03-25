package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a field extracted from OCR.
 */
public class OcrField {

    @JsonProperty("metadata_field_name")
    public final String name;

    @JsonProperty("metadata_field_value")
    public final String value;

    /**
     * Constructor for OcrField.
     * @param name field name
     * @param value field value
     */
    @JsonCreator
    public OcrField(
        @JsonProperty("metadata_field_name") String name,
        @JsonProperty("metadata_field_value") String value
    ) {
        this.name = name;
        this.value = value;
    }
}
