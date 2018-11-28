package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class OcrDataField {

    private String name;
    private ValueNode value;

    public String getName() {
        return name;
    }

    @JsonSetter(value = "metadata_field_name", nulls = Nulls.FAIL)
    public void setName(String name) {
        this.name = name;
    }

    public JsonNode getValue() {
        return value;
    }

    @JsonSetter(value = "metadata_field_value")
    public void setValue(ValueNode value) {
        this.value = value;
    }
}
