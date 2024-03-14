package uk.gov.hmcts.reform.bulkscanprocessor.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;

import java.util.List;

public class OcrData {

    @JsonProperty("Metadata_file")
    @JdbcType(BinaryJdbcType.class)
    public final List<OcrDataField> fields;

    public OcrData(@JsonProperty("Metadata_file")  List<OcrDataField> fields) {
        this.fields = fields;
    }
}
