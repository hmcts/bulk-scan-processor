package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class ErrorNotificationConfiguration {

    @Bean
    Decoder feignDecoder(ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(
        @Value("${error_notifications.username}") String username,
        @Value("${error_notifications.password}") String password
    ) {
        return new BasicAuthRequestInterceptor(username, password, Charsets.UTF_8);
    }
}
