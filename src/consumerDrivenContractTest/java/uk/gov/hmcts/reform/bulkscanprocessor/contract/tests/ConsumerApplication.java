package uk.gov.hmcts.reform.bulkscanprocessor.contract.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@ComponentScan(basePackages = "uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client")
public class ConsumerApplication {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
