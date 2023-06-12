package uk.gov.hmcts.reform.bulkscanprocessor.services.jms;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConfigurationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectingException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@ConditionalOnExpression("${jms.enabled}")
public class JmsErrorNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(JmsErrorNotificationSender.class);

    private final JmsQueueSendHelper notificationsJmsQueueHelper;

    private final ContainerMappings containerMappings;

    public JmsErrorNotificationSender(
        @Qualifier("jms-notifications-helper") JmsQueueSendHelper notificationsJmsQueueHelper,
        ContainerMappings containerMappings
    ) {
        this.notificationsJmsQueueHelper = notificationsJmsQueueHelper;
        this.containerMappings = containerMappings;
    }

    public void sendErrorNotification(
        String zipFilename,
        String containerName,
        EnvelopeRejectionException cause,
        Long eventId,
        ErrorCode errorCode
    ) {
        try {
            sendErrorMessageToQueue(zipFilename, containerName, eventId, errorCode, cause.getErrorDescription());
        } catch (Exception exc) {
            final String msg = "Error sending error notification to the queue."
                + "File name: " + zipFilename + " "
                + "Container: " + containerName;
            throw new EnvelopeRejectingException(msg, exc);
        }
    }

    private void sendErrorMessageToQueue(
        String zipFilename,
        String containerName,
        Long eventId,
        ErrorCode errorCode,
        String message
    ) {
        String messageId = StringUtils.joinWith("_", containerName, zipFilename);

        notificationsJmsQueueHelper.sendMessage(
            new ErrorMsg(
                messageId,
                eventId,
                zipFilename,
                containerName,
                getPoBoxes(containerName),
                null,
                errorCode,
                message,
                "bulk_scan_processor",
                containerName
            )
        );

        log.info(
            "Created error notification for file {} in container {}. Queue message ID: {}",
            zipFilename,
            containerName,
            messageId
        );
    }

    private String getPoBoxes(String containerName) {
        return containerMappings
            .getMappings()
            .stream()
            .filter(m -> m.getContainer().equals(containerName))
            .map(m -> String.join(",", m.getPoBoxes()))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException("Mapping not found for container " + containerName));
    }
}
