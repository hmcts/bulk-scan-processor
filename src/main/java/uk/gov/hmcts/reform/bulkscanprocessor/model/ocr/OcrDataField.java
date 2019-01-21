package uk.gov.hmcts.reform.bulkscanprocessor.model.ocr;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class OcrDataField {

    @JsonProperty(value = "metadata_field_name", required = true)
    private TextNode metadataFieldName;
    private ValueNode metadataFieldValue;

    public TextNode getMetadataFieldName() {
        return metadataFieldName;
    }

    @JsonSetter(value = "metadata_field_name", nulls = Nulls.FAIL)
    public void setMetadataFieldName(TextNode metadataFieldName) {
        this.metadataFieldName = metadataFieldName;
    }

    public ValueNode getMetadataFieldValue() {
        return metadataFieldValue;
    }

    @JsonSetter(value = "metadata_field_value")
    public void setMetadataFieldValue(ValueNode metadataFieldValue) {
        this.metadataFieldValue = metadataFieldValue;
    }
}
