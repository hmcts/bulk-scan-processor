package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

/**
 * Represents a field in OCR data.
 */
public class InputOcrDataField {

    public final TextNode name;

    public final ValueNode value;

    /**
     * Constructor for InputOcrDataField.
     * @param name the name of the field
     * @param value the value of the field
     */
    @JsonCreator
    public InputOcrDataField(
        @JsonProperty(value = "metadata_field_name", required = true) TextNode name,
        @JsonProperty(value = "metadata_field_value") ValueNode value
    ) {
        this.name = name;
        this.value = value;
    }

}
