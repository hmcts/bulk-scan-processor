package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req;

/**
 * Represents a field extracted from OCR.
 */
public class OcrDataField {

    public final String name;
    public final String value;

    /**
     * Constructor for OcrDataField.
     * @param name name of the field
     * @param value value of the field
     */
    public OcrDataField(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
