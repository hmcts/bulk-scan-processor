package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

@Service
public class ErrorNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationService.class);

    private final ErrorNotificationClient client;

    public ErrorNotificationService(
        ErrorNotificationClient client
    ) {
        this.client = client;
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

        ErrorNotificationResponse response = client.notify(request);

        // store result in db. wip
        log.info("Error notification published. ID: {}", response.getNotificationId());
    }
}
