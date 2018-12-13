package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

public class ErrorNotificationConfiguration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Bean
    Decoder feignDecoder() {
        return new JacksonDecoder(MAPPER);
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(
        @Value("${error_notifications.username}") String username,
        @Value("${error_notifications.password}") String password
    ) {
        return new BasicAuthRequestInterceptor(username, password, StandardCharsets.UTF_8);
    }

    @Bean
    public ErrorDecoder customErrorDecoder() {
        return new ErrorNotificationDecoder(MAPPER);
    }
}
