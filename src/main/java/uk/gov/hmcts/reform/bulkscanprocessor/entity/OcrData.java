package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

public class OcrData {

    private List<OcrDataField> fields;

    @JsonSetter(value = "Metadata_file", nulls = Nulls.FAIL)
    public void setFields(List<OcrDataField> fields) {
        this.fields = fields;
    }

    public List<OcrDataField> getFields() {
        return fields;
    }
}
