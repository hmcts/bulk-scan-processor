package uk.gov.hmcts.reform.bulkscanprocessor.services.jms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@ConditionalOnExpression("${jms.enabled}")
public class JmsFileRejector {

    private final BlobManager blobManager;

    private final JmsErrorNotificationSender errorNotificationSender;

    public JmsFileRejector(
        BlobManager blobManager,
        JmsErrorNotificationSender errorNotificationSender
    ) {
        this.blobManager = blobManager;
        this.errorNotificationSender = errorNotificationSender;
    }

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
