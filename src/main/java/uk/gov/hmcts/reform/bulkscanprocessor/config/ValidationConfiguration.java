package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@Configuration
public class ValidationConfiguration {

    @Bean
    public Validator jsonSchemaValidator() {
        return Validator.builder().failEarly().build();
    }

    @Bean
    public Consumer<JSONObject> jsonValidator(Validator validator) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/metadata-schema.json")) {
            Schema schema = SchemaLoader.load(
                new JSONObject(new JSONTokener(inputStream))
            );

            return (JSONObject json) -> validator.performValidation(schema, json);
        }
    }
}
