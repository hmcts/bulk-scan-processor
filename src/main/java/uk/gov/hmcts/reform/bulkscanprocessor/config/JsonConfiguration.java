package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class JsonConfiguration {

    @Bean
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        return mapper;
    }

    @Bean
    public JsonSchema jsonSchemaValidator(ObjectMapper mapper) throws IOException, ProcessingException {
        // library only supports up to draft-04 of json schema
        try (InputStream inputStream = getClass().getResourceAsStream("/metafile-schema.json")) {
            return JsonSchemaFactory
                .byDefault()
                .getJsonSchema(mapper.readTree(inputStream));
        }
    }
}
