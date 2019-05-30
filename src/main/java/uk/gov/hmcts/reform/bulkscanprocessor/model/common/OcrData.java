package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OcrData {

    @JsonProperty("Metadata_file")
    public final List<OcrDataField> fields;

    public OcrData(@JsonProperty("Metadata_file") List<OcrDataField> fields) {
        this.fields = fields;
    }
}
