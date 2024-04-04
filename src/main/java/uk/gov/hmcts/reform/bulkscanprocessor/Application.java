package uk.gov.hmcts.reform.bulkscanprocessor;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ServiceBusHelpersConfiguration;

@SpringBootApplication
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform"})
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    static LazyInitializationExcludeFilter lazyInitExcludeFilter() {
        return LazyInitializationExcludeFilter.forBeanTypes(
            ServiceBusHelpersConfiguration.class
        );
    }
}
