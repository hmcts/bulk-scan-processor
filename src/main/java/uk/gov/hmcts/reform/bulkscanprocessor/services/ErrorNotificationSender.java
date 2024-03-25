package uk.gov.hmcts.reform.bulkscanprocessor.services;

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
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;

/**
 * Sends error notifications to the error notification queue.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@ConditionalOnExpression("!${jms.enabled}")
public class ErrorNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationSender.class);

    private final ServiceBusSendHelper notificationsSendQueueHelper;

    private final ContainerMappings containerMappings;

    /**
     * Constructor for the ErrorNotificationSender.
     * @param notificationsQueueHelper The notifications queue helper
     * @param containerMappings The container mappings
     */
    public ErrorNotificationSender(
        @Qualifier("notifications-helper") ServiceBusSendHelper notificationsQueueHelper,
        ContainerMappings containerMappings
    ) {
        this.notificationsSendQueueHelper = notificationsQueueHelper;
        this.containerMappings = containerMappings;
    }

    /**
     * Sends an error notification to the error notification queue.
     * @param zipFilename The zip file name
     * @param containerName The container name
     * @param eventId The event ID
     * @param errorCode The error code
     * @param cause The cause of the error
     * @throws EnvelopeRejectingException If an error occurs while sending the error notification
     */
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

    /**
     * Sends an error notification to the error notification queue.
     * @param zipFilename The zip file name
     * @param containerName The container name
     * @param eventId The event ID
     * @param errorCode The error code
     * @param message The error message
     */
    private void sendErrorMessageToQueue(
        String zipFilename,
        String containerName,
        Long eventId,
        ErrorCode errorCode,
        String message
    ) {
        String messageId = StringUtils.joinWith("_", containerName, zipFilename);

        notificationsSendQueueHelper.sendMessage(
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

    /**
     * Gets the PO boxes for a given container.
     * @param containerName The container name
     * @return The PO boxes
     */
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
