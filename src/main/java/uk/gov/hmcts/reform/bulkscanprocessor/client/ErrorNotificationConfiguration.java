package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

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
        return new BasicAuthRequestInterceptor(username, password, StandardCharsets.UTF_8);
    }

    @Bean
    public RequestInterceptor logRequestBodyInterceptor() {
        // let's choose client itself where request "originates"
        Logger logger = LoggerFactory.getLogger(ErrorNotificationClient.class);

        return template -> {
            logger.info("Error notification body:\n{}", new String(template.body()));
        };
    }

    @Bean
    public ErrorDecoder customErrorDecoder(ObjectMapper objectMapper) {
        return new ErrorNotificationDecoder(objectMapper);
    }
}
