package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ErrorNotification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ErrorNotificationRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

import javax.persistence.EntityManager;

@Service
public class ErrorNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationService.class);

    private final ErrorNotificationClient client;

    private final ErrorNotificationRepository repository;

    private final EntityManager entityManager;

    public ErrorNotificationService(
        ErrorNotificationClient client,
        ErrorNotificationRepository repository,
        EntityManager entityManager
    ) {
        this.client = client;
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public void processServiceBusMessage(ErrorMsg message) {
        ErrorNotificationRequest request = new ErrorNotificationRequest(
            message.zipFileName,
            message.poBox,
            message.documentControlNumber,
            message.errorCode.name(),
            message.errorDescription,
            message.id
        );
        ErrorNotification entity = new ErrorNotification(
            entityManager.getReference(ProcessEvent.class, message.eventId),
            message.errorCode.name(),
            message.service
        );

        try {
            ErrorNotificationResponse response = client.notify(request);

            entity.setNotificationId(response.getNotificationId());

            log.info(
                "Error notification for file {} sent. Notification ID: {}",
                message.zipFileName,
                response.getNotificationId()
            );
        } finally {
            repository.saveAndFlush(entity);
        }
    }
}
