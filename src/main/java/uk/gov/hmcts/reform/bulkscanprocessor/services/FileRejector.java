package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

/**
 * Rejects invalid files.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@ConditionalOnExpression("!${jms.enabled}")
public class FileRejector {

    private final BlobManager blobManager;

    private final ErrorNotificationSender errorNotificationSender;

    /**
     * Constructor for the FileRejector.
     * @param blobManager The blob manager
     * @param errorNotificationSender The error notification sender
     */
    public FileRejector(
        BlobManager blobManager,
        ErrorNotificationSender errorNotificationSender
    ) {
        this.blobManager = blobManager;
        this.errorNotificationSender = errorNotificationSender;
    }

    /**
     * Handles invalid blob.
     * @param eventId The event ID
     * @param containerName The container name
     * @param zipFilename The zip filename
     * @param cause The cause
     */
    public void handleInvalidBlob(
        Long eventId,
        String containerName,
        String zipFilename,
        EnvelopeRejectionException cause
    ) {
        errorNotificationSender.sendErrorNotification(
            zipFilename,
            containerName,
            cause,
            eventId,
            cause.getErrorCode()
        );
        blobManager.tryMoveFileToRejectedContainer(zipFilename, containerName);
    }
}
