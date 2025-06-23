package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper
            .builder()
            .addModule(new JavaTimeModule())
            .disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)
            .build();
    }
}
