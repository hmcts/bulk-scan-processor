package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the data extracted from OCR.
 */
public class FormData {

    @JsonProperty("ocr_data_fields")
    public final List<OcrDataField> ocrDataFields;

    /**
     * Constructo for FormData.
     * @param ocrDataFields list of OCR data fields
     */
    public FormData(List<OcrDataField> ocrDataFields) {
        this.ocrDataFields = ocrDataFields;
    }
}
