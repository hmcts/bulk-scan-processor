package uk.gov.hmcts.reform.bulkscanprocessor.model.blob;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

/**
 * Represents OCR data in a document.
 */
public class InputOcrData {

    private List<InputOcrDataField> fields;

    /**
     * Constructor for InputOcrData.
     * @param fields the fields
     */
    @JsonSetter(value = "Metadata_file", nulls = Nulls.FAIL)
    public void setFields(List<InputOcrDataField> fields) {
        this.fields = fields;
    }

    /**
     * Gets the fields.
     * @return the fields
     */
    public List<InputOcrDataField> getFields() {
        return fields;
    }
}
