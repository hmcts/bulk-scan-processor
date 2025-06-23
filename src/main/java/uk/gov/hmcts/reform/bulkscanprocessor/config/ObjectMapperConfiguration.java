package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper
            .builder()
            .disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)
            .build();
    }
}
