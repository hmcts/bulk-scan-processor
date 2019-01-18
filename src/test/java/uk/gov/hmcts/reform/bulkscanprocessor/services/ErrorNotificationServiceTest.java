package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ErrorNotification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ErrorNotificationRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ErrorNotificationServiceTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Captor
    public ArgumentCaptor<ErrorNotificationRequest> requestCaptor;

    @Mock
    private ErrorNotificationClient client;

    @Mock
    private ErrorNotificationRepository repository;

    @Mock
    private EntityManager entityManager;

    private ErrorNotificationService service;

    @Before
    public void setUp() {
        capture.reset();
        service = new ErrorNotificationService(client, repository, entityManager);
    }

    @After
    public void tearDown() {
        capture.flush();
    }

    @Test
    public void should_save_to_db_and_log_the_response_when_notification_sent_to_client() {
        // given
        ErrorMsg serviceBusMessage = new ErrorMsg(
            "some id",
            0L,
            "zip file name",
            "jurisdiction",
            "po box",
            "document control number",
            ErrorCode.ERR_AV_FAILED,
            "antivirus flag"
        );
        ErrorNotificationResponse response = new ErrorNotificationResponse("notify id");
        given(client.notify(requestCaptor.capture())).willReturn(response);

        // when
        service.processServiceBusMessage(serviceBusMessage);

        // then
        ErrorNotificationRequest request = requestCaptor.getValue();
        assertThat(request.zipFileName).isEqualTo(serviceBusMessage.zipFileName);
        assertThat(request.poBox).isEqualTo(serviceBusMessage.poBox);
        assertThat(request.documentControlNumber).isEqualTo(serviceBusMessage.documentControlNumber);
        assertThat(request.errorCode).isEqualTo(serviceBusMessage.errorCode.name());
        assertThat(request.errorDescription).isEqualTo(serviceBusMessage.errorDescription);
        assertThat(request.referenceId).isEqualTo(serviceBusMessage.id);

        // and
        assertThat(capture.toString()).contains(
            "Error notification for " + request.zipFileName + " published. ID: " + response.getNotificationId()
        );
        verify(repository).save(any(ErrorNotification.class));
    }

    public void should_save_to_db_and_log_the_failure_when_notification_is_attempted() {
        // given
        ErrorMsg serviceBusMessage = new ErrorMsg(
            "some id",
            0L,
            "zip file name",
            "jurisdiction",
            "po box",
            "document control number",
            ErrorCode.ERR_AV_FAILED,
            "antivirus flag"
        );
        given(client.notify(any(ErrorNotificationRequest.class))).willThrow(Exception.class);

        // when
        service.processServiceBusMessage(serviceBusMessage);

        // then
        assertThat(capture.toString()).contains("Failed to publish error notification.");
        verify(repository).save(any(ErrorNotification.class));
    }
}
