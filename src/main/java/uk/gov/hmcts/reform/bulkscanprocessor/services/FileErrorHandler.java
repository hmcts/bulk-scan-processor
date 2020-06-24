package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class FileErrorHandler {

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
        EnvelopeRejectionException cause
    ) {
        Long eventId = envelopeProcessor.createEvent(
            fileValidationFailure,
            containerName,
            zipFilename,
            cause.getMessage(),
            null
        );

        errorNotificationSender.sendErrorNotification(
            zipFilename,
            containerName,
            cause,
            eventId,
            cause.getErrorCode()
        );
        blobManager.tryMoveFileToRejectedContainer(zipFilename, containerName, leaseId);
    }
}
