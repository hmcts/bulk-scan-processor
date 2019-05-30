package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OcrData {

    @JsonProperty("Metadata_file")
    public final List<OcrDataField> fields;

    public OcrData(List<OcrDataField> fields) {
        this.fields = fields;
    }
}
