package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ErrorNotificationRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers.ErrorNotificationExceptionHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationFailingResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_FILE_LIMIT_EXCEEDED;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ErrorNotificationHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ZIP_FILE_NAME = "zip_file_name";

    private static final String MESSAGE_ID = UUID.randomUUID().toString();

    private static final String NOTIFICATION_ID = "notification ID";

    private static final CompletableFuture<Void> EMPTY_FUTURE = CompletableFuture.completedFuture(null);

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Autowired
    private ErrorNotificationRepository notificationRepository;

    @Autowired
    private ProcessEventRepository eventRepository;

    @Mock
    private IQueueClient queueClient;

    @MockBean
    private ErrorNotificationClient notificationClient;

    @Autowired
    private ErrorNotificationService notificationService;

    private ErrorNotificationHandler notificationHandler;

    @Before
    public void setUp() {
        notificationHandler = new ErrorNotificationHandler(
            notificationService,
            OBJECT_MAPPER,
            new MessageAutoCompletor(queueClient)
        );

        outputCapture.reset();
    }

    @After
    public void tearDown() {
        notificationRepository.deleteAll();
        eventRepository.deleteAll();
        outputCapture.flush();
    }

    @Test
    public void should_void_the_message_when_5xx_error_received_from_notification_client()
        throws JsonProcessingException {
        // given
        IMessage message = getServiceBusMessage();

        // and
        given(notificationClient.notify(any(ErrorNotificationRequest.class)))
            .willThrow(new ErrorNotificationException(
                new HttpServerErrorException(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getReasonPhrase()),
                null
            ));

        // when
        CompletableFuture<Void> future = notificationHandler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        String output = outputCapture.toString();

        assertThat(output).containsPattern("INFO.+"
            + ErrorNotificationHandler.class.getCanonicalName()
            + ":\\d+: Processing error notification for "
            + ZIP_FILE_NAME
        );
        assertThat(output).containsPattern("WARN  \\[error-notification-handler\\] "
            + ErrorNotificationExceptionHandler.class.getCanonicalName()
            + ":\\d+: Received server error from notification client. Voiding message \\(ID: "
            + MESSAGE_ID
            + "\\) after 1 delivery attempt\n"
        );
        assertThat(output).doesNotContainPattern("INFO  \\[error-notification-handler\\] "
            + ErrorNotificationExceptionHandler.class.getCanonicalName()
            + ":\\d+: Error occurred when posting notification. Parsed response:"
        );
        assertThat(output).doesNotContainPattern("INFO  \\[error-notification-handler\\] "
            + ErrorNotificationExceptionHandler.class.getCanonicalName()
            + ":\\d+: Error occurred when posting notification. Raw response:"
        );
        assertThat(output).containsPattern("Caused by: "
            + ErrorNotificationException.class.getCanonicalName()
            + ": "
            + HttpServerErrorException.class.getCanonicalName()
            + ": 500 Internal Server Error\n"
        );
    }

    @Test
    public void should_dlq_the_message_when_4xx_error_received_from_notification_client()
        throws JsonProcessingException {
        // given
        IMessage message = getServiceBusMessage();

        // and
        given(notificationClient.notify(any(ErrorNotificationRequest.class)))
            .willThrow(new ErrorNotificationException(
                new HttpClientErrorException(
                    BAD_REQUEST,
                    BAD_REQUEST.getReasonPhrase(),
                    "some body".getBytes(),
                    StandardCharsets.UTF_8
                ),
                new ErrorNotificationFailingResponse("doh")
            ));
        given(queueClient.deadLetterAsync(any(UUID.class), eq("Client error"), anyString()))
            .willReturn(EMPTY_FUTURE);

        // when
        CompletableFuture<Void> future = notificationHandler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        String output = outputCapture.toString();

        assertThat(output).containsPattern("INFO.+"
            + ErrorNotificationHandler.class.getCanonicalName()
            + ":\\d+: Processing error notification for "
            + ZIP_FILE_NAME
        );
        assertThat(output).containsPattern("ERROR \\[error-notification-handler\\] "
            + ErrorNotificationExceptionHandler.class.getCanonicalName()
            + ":\\d+: Client error. Dead lettering message \\(ID: "
            + MESSAGE_ID
            + "\\)\n"
        );
        assertThat(output).containsPattern("INFO  \\[error-notification-handler\\] "
            + ErrorNotificationExceptionHandler.class.getCanonicalName()
            + ":\\d+: Error occurred when posting notification. "
            + "Parsed response: doh. "
            + "Raw response: some body"
        );
        assertThat(output).containsPattern("Caused by: "
            + ErrorNotificationException.class.getCanonicalName()
            + ": "
            + HttpClientErrorException.class.getCanonicalName()
            + ": 400 Bad Request\n"
        );
    }

    @Test
    public void should_complete_the_message_when_success_received_from_notification_client()
        throws JsonProcessingException {
        // given
        IMessage message = getServiceBusMessage();

        // and
        given(notificationClient.notify(any(ErrorNotificationRequest.class)))
            .willReturn(new ErrorNotificationResponse(NOTIFICATION_ID));
        given(queueClient.completeAsync(any(UUID.class))).willReturn(EMPTY_FUTURE);

        // when
        CompletableFuture<Void> future = notificationHandler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        String output = outputCapture.toString();

        assertThat(output).containsPattern("INFO.+"
            + ErrorNotificationHandler.class.getCanonicalName()
            + ":\\d+: Processing error notification for "
            + ZIP_FILE_NAME
        );
        assertThat(output).containsPattern("INFO  \\[error-notification-service\\] "
            + ErrorNotificationService.class.getCanonicalName()
            + ":\\d+: Error notification for "
            + ZIP_FILE_NAME
            + " published. ID: "
            + NOTIFICATION_ID
        );
    }

    private IMessage getServiceBusMessage() throws JsonProcessingException {
        ProcessEvent event = eventRepository.save(
            new ProcessEvent("container_name", ZIP_FILE_NAME, Event.DOC_FAILURE)
        );

        IMessage message = spy(new Message(
            MESSAGE_ID,
            OBJECT_MAPPER.writeValueAsBytes(
                new ErrorMsg(
                    "id",
                    event.getId(),
                    ZIP_FILE_NAME,
                    "jurisdiction",
                    "po_box",
                    null,
                    ERR_FILE_LIMIT_EXCEEDED,
                    "description"
                )
            ),
            MediaType.APPLICATION_JSON_UTF8_VALUE
        ));

        given(message.getLockToken()).willReturn(UUID.randomUUID());
        given(message.getDeliveryCount()).willReturn(0L);

        return message;
    }
}
