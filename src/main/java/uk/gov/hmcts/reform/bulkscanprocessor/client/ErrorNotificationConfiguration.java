package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.context.annotation.Bean;

public class ErrorNotificationConfiguration {

    @Bean
    Decoder feignDecoder(ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor("user", "password");
    }
}
