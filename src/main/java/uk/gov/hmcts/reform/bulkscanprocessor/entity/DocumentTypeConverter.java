package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import javax.persistence.AttributeConverter;

public class DocumentTypeConverter implements AttributeConverter<DocumentType, String> {

    @Override
    public String convertToDatabaseColumn(DocumentType attribute) {
        return attribute.name().toLowerCase();
    }

    @Override
    public DocumentType convertToEntityAttribute(String dbData) {
        return DocumentType.valueOf(dbData.toUpperCase());
    }
}
