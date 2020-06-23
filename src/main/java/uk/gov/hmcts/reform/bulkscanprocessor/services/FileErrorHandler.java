package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConfigurationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.errornotifications.ErrorMapping;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class FileErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(FileErrorHandler.class);

    private final BlobManager blobManager;

    private final EnvelopeProcessor envelopeProcessor;

    private final ErrorNotificationSender errorNotificationSender;

    public FileErrorHandler(
        BlobManager blobManager,
        EnvelopeProcessor envelopeProcessor,
        ErrorNotificationSender errorNotificationSender
    ) {
        this.blobManager = blobManager;
        this.envelopeProcessor = envelopeProcessor;
        this.errorNotificationSender = errorNotificationSender;
    }

    public void handleInvalidFileError(
        Event fileValidationFailure,
        String containerName,
        String zipFilename,
        String leaseId,
        Exception cause
    ) {
        Long eventId = envelopeProcessor.createEvent(
            fileValidationFailure,
            containerName,
            zipFilename,
            cause.getMessage(),
            null
        );

        Optionals.ifPresentOrElse(
            ErrorMapping.getFor(cause),
            (errorCode) -> {
                errorNotificationSender.sendErrorNotification(
                    zipFilename,
                    containerName,
                    cause,
                    eventId,
                    errorCode
                );
                blobManager.tryMoveFileToRejectedContainer(zipFilename, containerName, leaseId);
            },
            () -> {
                log.error(
                    "Error notification not sent because Error code mapping not found for {}. "
                        + "File name: {} Container: {} Reason: {}",
                    cause.getClass().getName(),
                    zipFilename,
                    containerName,
                    cause
                );
                throw new ConfigurationException("Error code mapping not found for " + cause.getClass().getName());
            }
        );
    }
}
