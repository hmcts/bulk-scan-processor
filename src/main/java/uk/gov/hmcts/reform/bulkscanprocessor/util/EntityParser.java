package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.io.IOException;
import java.io.InputStream;

public final class EntityParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Envelope parseEnvelopeMetadata(InputStream metadataStream) throws IOException {
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        try {
            return mapper.readValue(metadataStream, Envelope.class);
        } finally {
            metadataStream.close();
        }
    }

    private EntityParser() {
        // utility class constructor
    }
}
