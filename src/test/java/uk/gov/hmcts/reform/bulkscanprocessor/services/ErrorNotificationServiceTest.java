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
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ErrorNotificationServiceTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Captor
    public ArgumentCaptor<ErrorNotificationRequest> requestCaptor;

    @Mock
    private ErrorNotificationClient client;

    private ErrorNotificationService service;

    @Before
    public void setUp() {
        capture.reset();
        service = new ErrorNotificationService(client);
    }

    @After
    public void tearDown() {
        capture.flush();
    }

    @Test
    public void should_send_notification_to_client_and_log_the_response() {
        // given
        ErrorMsg serviceBusMessage = new ErrorMsg(
            "some id",
            null,
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
            "Error notification published. ID: " + response.getNotificationId()
        );
    }
}
