package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

@AutoConfigureAfter(ServiceBusHelpersConfiguration.class)
@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
public class MessageHandlerConfig {

    public static final Logger log = LoggerFactory.getLogger(MessageHandlerConfig.class);

    @Autowired
    @Qualifier("processed-envelopes-client")
    public ServiceBusProcessorClient processedEnvelopesQueueClient;

    @PostConstruct()
    public void registerMessageHandlers() {
        processedEnvelopesQueueClient.start();
    }
}
