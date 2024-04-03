package uk.gov.hmcts.reform.bulkscanprocessor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger configuration.
 */
@Configuration
public class SwaggerConfiguration {

    /**
     * Bean for OpenAPI.
     * @return The OpenAPI
     */
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(
                new Info().title("Bulk Scan Processor")
                    .description("Bulk Scan Processor handlers")
                    .version("v0.0.1")
            );
    }
}
