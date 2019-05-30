package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class OcrDataField {

    @JsonProperty("metadata_field_name")
    public final TextNode name;

    @JsonProperty("metadata_field_value")
    public final ValueNode value;

    public OcrDataField(
        @JsonProperty("metadata_field_name") TextNode name,
        @JsonProperty("metadata_field_value") ValueNode value
    ) {
        this.name = name;
        this.value = value;
    }

}
