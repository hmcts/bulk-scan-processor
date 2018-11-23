package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;

public class DocumentTypeConverter implements AttributeConverter<DocumentType, String> {

    private static final Logger log = LoggerFactory.getLogger(DocumentTypeConverter.class);

    @Override
    public String convertToDatabaseColumn(DocumentType attribute) {
        return attribute.name().toLowerCase();
    }

    @Override
    public DocumentType convertToEntityAttribute(String dbData) {
        try {
            return DocumentType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException exception) {
            // for backwards compatibility due to a lot of incorrect data in db
            log.warn("Invalid document type found in DB: '{}'", dbData, exception);

            return DocumentType.OTHER;
        }
    }
}
