package uk.gov.hmcts.reform.bulkscanprocessor.model.ocr;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class OcrDataField {

    private TextNode name;
    private ValueNode value;

    public TextNode getName() {
        return name;
    }

    @JsonSetter(value = "metadata_field_name", nulls = Nulls.FAIL)
    public void setName(TextNode name) {
        this.name = name;
    }

    public ValueNode getValue() {
        return value;
    }

    @JsonSetter(value = "metadata_field_value")
    public void setValue(ValueNode value) {
        this.value = value;
    }
}
