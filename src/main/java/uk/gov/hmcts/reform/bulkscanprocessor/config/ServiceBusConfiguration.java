package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Represents Spring configuration class related with Azure Service Bus.
 *
 * <p>
 * Configuration classes with this annotation are only applied
 * when interactions with Service Bus are enabled (i.e. active profile is not "nosb")
 * </p>
 */
@Configuration
@Profile("!nosb")
@Target(ElementType.TYPE)
public @interface ServiceBusConfiguration {
}
