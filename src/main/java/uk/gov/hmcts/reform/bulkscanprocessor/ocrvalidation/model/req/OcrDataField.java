package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.model.req;

public class OcrDataField {

    public final String name;
    public final String value;

    public OcrDataField(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
