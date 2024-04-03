package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;

import java.util.List;

/**
 * Represents OCR data extracted from a document.
 */
public class OcrData {

    @JsonProperty("Metadata_file")
    @JdbcType(BinaryJdbcType.class)
    public final List<OcrDataField> fields;

    /**
     * Constructor.
     * @param fields The fields
     */
    public OcrData(@JsonProperty("Metadata_file")  List<OcrDataField> fields) {
        this.fields = fields;
    }
}
