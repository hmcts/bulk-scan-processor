package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class FormData {

    @JsonProperty("ocr_data_fields")
    public final List<OcrDataField> ocrDataFields;

    public FormData(List<OcrDataField> ocrDataFields) {
        this.ocrDataFields = ocrDataFields;
    }
}
