package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class FileRejector {

    private final BlobManager blobManager;

    private final ErrorNotificationSender errorNotificationSender;

    public FileRejector(
        BlobManager blobManager,
        ErrorNotificationSender errorNotificationSender
    ) {
        this.blobManager = blobManager;
        this.errorNotificationSender = errorNotificationSender;
    }

    public void handleInvalidBlob(
        Long eventId,
        String containerName,
        String zipFilename,
        String leaseId,
        EnvelopeRejectionException cause
    ) {
        errorNotificationSender.sendErrorNotification(
            zipFilename,
            containerName,
            cause,
            eventId,
            cause.getErrorCode()
        );
        blobManager.newTryMoveFileToRejectedContainer(zipFilename, containerName, leaseId);
    }
}
