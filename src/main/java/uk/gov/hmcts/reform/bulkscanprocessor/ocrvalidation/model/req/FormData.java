package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FormData {

    @JsonProperty("form_type")
    public final String type;

    @JsonProperty("ocr_data_fields")
    public final List<OcrDataField> ocrDataFields;

    public FormData(
        String type,
        List<OcrDataField> ocrDataFields
    ) {
        this.type = type;
        this.ocrDataFields = ocrDataFields;
    }
}
