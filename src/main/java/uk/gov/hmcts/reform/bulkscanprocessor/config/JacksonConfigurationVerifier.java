package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JacksonConfigurationVerifier {

    @Autowired
    public JacksonConfigurationVerifier(ObjectMapper objectMapper) {
        if (objectMapper.getFactory().isEnabled(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)) {
            throw new IllegalStateException("Jackson ObjectMapper is configured with AUTO_CLOSE_JSON_CONTENT "
                                                + "enabled. This can cause issues with "
                                                + "streaming JSON responses an must be disabled.");
        }
    }
}
