package uk.gov.hmcts.reform.bulkscanprocessor.config.jms;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultRedeliveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Configuration for JMS queues.
 */
@Configuration
@EnableJms
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsQueueClientConfig {

    @Value("${jms.namespace}")
    private String namespace;

    @Value("${jms.username}")
    private String username;

    @Value("${jms.password}")
    private String password;

    @Value("${jms.receiveTimeout}")
    private Long receiveTimeout;

    @Value("${jms.idleTimeout}")
    private Long idleTimeout;

    @Value("${jms.amqp-connection-string-template}")
    public String amqpConnectionStringTemplate;

    @Value("${jms.application-name}")
    public String clientId;

    /**
     * Bean for JmsConnectionFactory.
     * @param clientId The client id
     */
    @Bean
    public ConnectionFactory processorJmsConnectionFactory(@Value("${jms.application-name}") final String clientId) {
        String connection = String.format(amqpConnectionStringTemplate, namespace, idleTimeout);
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connection);
        jmsConnectionFactory.setUsername(username);
        jmsConnectionFactory.setPassword(password);
        JmsDefaultRedeliveryPolicy jmsDefaultRedeliveryPolicy = new JmsDefaultRedeliveryPolicy();
        jmsDefaultRedeliveryPolicy.setMaxRedeliveries(3);
        jmsConnectionFactory.setRedeliveryPolicy(jmsDefaultRedeliveryPolicy);
        jmsConnectionFactory.setClientID(clientId);
        return new CachingConnectionFactory(jmsConnectionFactory);
    }

    /**
     * Bean for JmsTemplate.
     * @param connectionFactory The connection factory
     * @return The JmsTemplate
     */
    @Bean(name = "envelopes-jms-template")
    public JmsTemplate envelopesJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setDefaultDestinationName("envelopes");
        jmsTemplate.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds
        return jmsTemplate;
    }

    /**
     * Bean for JmsTemplate.
     * @param connectionFactory The connection factory
     * @return The JmsTemplate
     */
    @Bean(name = "notifications-jms-template")
    public JmsTemplate notificationsJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setDefaultDestinationName("notifications");
        jmsTemplate.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds
        return jmsTemplate;
    }

    /**
     * Bean for JmsListenerContainerFactory.
     * @param processorJmsConnectionFactory The connection factory
     * @return The JmsListenerContainerFactory
     */
    @Bean
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> processedEnvelopesQueueConnectionFactory(
        ConnectionFactory processorJmsConnectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(2);
        factory.setConnectionFactory(processorJmsConnectionFactory);
        factory.setReceiveTimeout(receiveTimeout);
        factory.setSessionTransacted(Boolean.TRUE);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        factory.setMessageConverter(new CustomMessageConverter());
        return factory;
    }

    /**
     * Custom message converter for JMS.
     */
    @Component
    public static class CustomMessageConverter implements MessageConverter {

        /**
         * Converts an object to a JMS message.
         * @param object The object
         * @param session The session
         * @return The JMS message
         * @throws JMSException If an error occurs
         * @throws MessageConversionException If an error occurs
         */
        @Override
        public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
            return session.createTextMessage(object.toString());
        }

        /**
         * Converts a JMS message to an object.
         * @param message The JMS message
         * @return The object
         * @throws MessageConversionException If an error occurs
         */
        @Override
        public Object fromMessage(Message message) throws MessageConversionException {
            return message;
        }
    }
}
