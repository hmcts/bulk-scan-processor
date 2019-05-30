package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class InputOcrDataField {

    @JsonProperty("metadata_field_name")
    public final TextNode name;

    @JsonProperty("metadata_field_value")
    public final ValueNode value;

    @JsonCreator
    public InputOcrDataField(
        @JsonProperty(value = "metadata_field_name", required = true) TextNode name,
        @JsonProperty(value = "metadata_field_value") ValueNode value
    ) {
        this.name = name;
        this.value = value;
    }

}
