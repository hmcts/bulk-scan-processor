package uk.gov.hmcts.reform.bulkscanprocessor.model.ocr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class OcrDataField {

    @JsonProperty(value = "metadata_field_name")
    private TextNode name;

    @JsonProperty(value = "metadata_field_value")
    private ValueNode value;

    @JsonCreator
    public OcrDataField(
        @JsonProperty(value = "metadata_field_name", required = true) TextNode name,
        @JsonProperty(value = "metadata_field_value") ValueNode value
    ) {
        this.name = name;
        this.value = value;
    }

}
