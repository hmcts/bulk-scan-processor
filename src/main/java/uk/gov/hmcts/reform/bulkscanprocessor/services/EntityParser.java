package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.io.IOException;
import java.io.InputStream;

@Service
public class EntityParser {

    private final ObjectMapper mapper;

    public EntityParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Envelope parseEnvelopeMetadata(InputStream metadataStream) throws IOException {
        try {
            return mapper.readValue(metadataStream, Envelope.class);
        } finally {
            metadataStream.close();
        }
    }
}
