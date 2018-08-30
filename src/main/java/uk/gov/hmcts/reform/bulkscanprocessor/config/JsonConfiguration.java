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

    /**
     * Default {@code ObjectMapper} used in the application.
     * This singleton is not injected in Spring Framework as per it's design.
     * The need of the bean arose from the time when metafile schema validation had to be in place.
     * Since application is using jackson the recommended validation library has tight dependency on validity calls.
     * In order to test everything properly with same configuration we introduce a bean and re-use:
     * <ul>
     * <li>{@code JsonSchema} validator {@link #metafileSchemaValidator(ObjectMapper)}</li>
     * <li>{@code EnvelopeProcessor} where we parse the metafile for inspection/conversion</li>
     * <li>Various unit/integration tests verifying our application</li>
     * </ul>
     * @return default object mapper per application.
     */
    @Bean
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        return mapper;
    }

    /**
     * Instance of metafile schema validator.
     * {@code JsonSchema} is in fact validator.
     * Library is designed this way to provide an {@code interface JsonSchema}
     * which is meant to hold the information about the schema to validate {@code JsonNode} objects against it.
     * @param mapper Default mapper specified in {@link #defaultObjectMapper()}
     * @return Configured metafile schema validator.
     * @throws IOException Caused by accessing/reading schema resource file.
     * @throws ProcessingException Caused by schema itself in case there are errors in the json file.
     */
    @Bean
    public JsonSchema metafileSchemaValidator(ObjectMapper mapper) throws IOException, ProcessingException {
        // library only supports up to draft-04 of json schema
        try (InputStream inputStream = getClass().getResourceAsStream("/metafile-schema.json")) {
            return JsonSchemaFactory
                .byDefault()
                .getJsonSchema(mapper.readTree(inputStream));
        }
    }
}
