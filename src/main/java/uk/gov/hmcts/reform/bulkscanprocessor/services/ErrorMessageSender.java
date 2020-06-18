package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConfigurationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectingException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.UUID;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class ErrorMessageSender {
    private static final Logger log = LoggerFactory.getLogger(ErrorMessageSender.class);

    private final ServiceBusHelper notificationsQueueHelper;

    private final ContainerMappings containerMappings;

    public ErrorMessageSender(
        @Qualifier("notifications-helper") ServiceBusHelper notificationsQueueHelper,
        ContainerMappings containerMappings
    ) {
        this.notificationsQueueHelper = notificationsQueueHelper;
        this.containerMappings = containerMappings;
    }

    public void sendErrorMessage(
        String zipFilename,
        String containerName,
        Exception cause,
        Long eventId,
        ErrorCode errorCode
    ) {
        try {
            String message = cause instanceof OcrValidationException
                ? ((OcrValidationException) cause).getDetailMessage()
                : cause.getMessage();
            sendErrorMessageToQueue(zipFilename, containerName, eventId, errorCode, message);
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
        String messageId = UUID.randomUUID().toString();

        notificationsQueueHelper.sendMessage(
            new ErrorMsg(
                messageId,
                eventId,
                zipFilename,
                containerName,
                getPoBox(containerName),
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

    private String getPoBox(String containerName) {
        return containerMappings
            .getMappings()
            .stream()
            .filter(m -> m.getContainer().equals(containerName))
            .map(ContainerMappings.Mapping::getPoBox)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException("Mapping not found for container " + containerName));
    }
}
