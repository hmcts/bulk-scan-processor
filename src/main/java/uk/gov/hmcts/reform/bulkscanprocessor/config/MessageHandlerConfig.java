package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


/**
 * Configuration for message handlers.
 */
@AutoConfigureAfter(ServiceBusHelpersConfiguration.class)
@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
@ConditionalOnExpression("!${jms.enabled}")
public class MessageHandlerConfig {

    public static final Logger log = LoggerFactory.getLogger(MessageHandlerConfig.class);

    @Autowired
    @Qualifier("processed-envelopes-client")
    public ServiceBusProcessorClient processedEnvelopesQueueClient;

    /**
     * Registers message handlers.
     */
    @PostConstruct()
    public void registerMessageHandlers() {
        processedEnvelopesQueueClient.start();
    }
}
