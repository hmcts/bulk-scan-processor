package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.io.IOException;
import java.io.InputStream;

public final class EntityParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static EnvelopeResponse parseEnvelopeMetadata(InputStream metadataStream) throws IOException {
        try {
            return mapper.readValue(metadataStream, EnvelopeResponse.class);
        } finally {
            metadataStream.close();
        }
    }

    private EntityParser() {
        // utility class constructor
    }
}
