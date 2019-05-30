package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

public class InputOcrData {

    private List<InputOcrDataField> fields;

    @JsonSetter(value = "Metadata_file", nulls = Nulls.FAIL)
    public void setFields(List<InputOcrDataField> fields) {
        this.fields = fields;
    }

    public List<InputOcrDataField> getFields() {
        return fields;
    }
}
