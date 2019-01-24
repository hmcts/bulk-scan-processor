package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.joran.spi.JoranException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
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
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestAppender;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_FILE_LIMIT_EXCEEDED;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = "logging.config=src/integrationTest/resources/logback-test-sender.xml"
)
public class ErrorNotificationHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ZIP_FILE_NAME = "zip_file_name";

    private static final String MESSAGE_ID = UUID.randomUUID().toString();

    private static final String NOTIFICATION_ID = "notification ID";

    private static final CompletableFuture<Void> EMPTY_FUTURE = CompletableFuture.completedFuture(null);

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
    public void setUp() throws JoranException, IOException {
        notificationHandler = new ErrorNotificationHandler(
            notificationService,
            OBJECT_MAPPER,
            new MessageAutoCompletor(queueClient)
        );

        // configure log
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();

        InputStream configStream = ResourceUtils.getURL("classpath:logback-test-receiver.xml").openStream();
        configurator.setContext(loggerContext);
        configurator.doConfigure(configStream);
        configStream.close();
    }

    @After
    public void tearDown() {
        notificationRepository.deleteAll();
        eventRepository.deleteAll();
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
                new ErrorNotificationFailingResponse("doh")
            ));

        // when
        CompletableFuture<Void> future = notificationHandler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        List<ILoggingEvent> events = loggedEvents();

        boolean matchedEntry = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationHandler.class.getCanonicalName()))
            .anyMatch(this::matchEventEntry);
        boolean matchedVoidEvent = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationExceptionHandler.class.getCanonicalName()))
            .anyMatch(this::matchVoidEvent);
        boolean matchedClosingEvent = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationHandler.class.getCanonicalName()))
            .anyMatch(this::matchClosingEvent);

        assertThat(matchedEntry).isTrue();
        assertThat(matchedVoidEvent).isTrue();
        assertThat(matchedClosingEvent).isTrue();
    }

    @Test
    public void should_dlq_the_message_when_4xx_error_received_from_notification_client()
        throws JsonProcessingException {
        // given
        IMessage message = getServiceBusMessage();

        // and
        given(notificationClient.notify(any(ErrorNotificationRequest.class)))
            .willThrow(new ErrorNotificationException(
                new HttpClientErrorException(BAD_REQUEST, BAD_REQUEST.getReasonPhrase()),
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
        List<ILoggingEvent> events = loggedEvents();

        boolean matchedEntry = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationHandler.class.getCanonicalName()))
            .anyMatch(this::matchEventEntry);
        boolean matchedDlqEvent = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationExceptionHandler.class.getCanonicalName()))
            .anyMatch(this::matchDlqEvent);
        boolean matchedClosingEvent = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationHandler.class.getCanonicalName()))
            .anyMatch(this::matchClosingEvent);

        assertThat(matchedEntry).isTrue();
        assertThat(matchedDlqEvent).isTrue();
        assertThat(matchedClosingEvent).isTrue();
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
        List<ILoggingEvent> events = loggedEvents();

        boolean matchedEntry = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationHandler.class.getCanonicalName()))
            .anyMatch(this::matchEventEntry);
        boolean matchedSuccessEvent = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationService.class.getCanonicalName()))
            .anyMatch(this::matchSuccessEvent);
        boolean matchedClosingEvent = events.stream()
            .filter(event -> event.getLoggerName().equals(ErrorNotificationHandler.class.getCanonicalName()))
            .anyMatch(this::matchClosingEvent);

        assertThat(matchedEntry).isTrue();
        assertThat(matchedSuccessEvent).isTrue();
        assertThat(matchedClosingEvent).isTrue();
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

    private boolean matchEventEntry(ILoggingEvent event) {
        return event.getLevel().equals(Level.INFO)
            && event.getFormattedMessage().equals("Processing error notification for " + ZIP_FILE_NAME);
    }

    private boolean matchVoidEvent(ILoggingEvent event) {
        String message = "Received server error from notification client."
            + " Voiding message (ID: " + MESSAGE_ID + ") after 1 delivery attempt";
        IThrowableProxy throwableProxy = event.getThrowableProxy();

        boolean eventMessageCheck = event.getLevel().equals(Level.WARN)
            && event.getFormattedMessage().equals(message);
        boolean eventThrowableCheck = throwableProxy != null
            && throwableProxy.getClassName().equals(ErrorNotificationException.class.getCanonicalName())
            && throwableProxy.getMessage()
                .equals(HttpServerErrorException.class.getCanonicalName() + ": 500 Internal Server Error");

        return eventMessageCheck && eventThrowableCheck;
    }

    private boolean matchDlqEvent(ILoggingEvent event) {
        String message = "Client error. Dead lettering message (ID: " + MESSAGE_ID + ")";
        IThrowableProxy throwableProxy = event.getThrowableProxy();

        boolean eventMessageCheck = event.getLevel().equals(Level.ERROR)
            && event.getFormattedMessage().equals(message);
        boolean eventThrowableCheck = throwableProxy != null
            && throwableProxy.getClassName().equals(ErrorNotificationException.class.getCanonicalName())
            && throwableProxy.getMessage()
            .equals(HttpClientErrorException.class.getCanonicalName() + ": 400 Bad Request");

        return eventMessageCheck && eventThrowableCheck;
    }

    private boolean matchSuccessEvent(ILoggingEvent event) {
        String message = "Error notification for " + ZIP_FILE_NAME + " published. ID: " + NOTIFICATION_ID;
        return event.getLevel().equals(Level.INFO)
            && event.getFormattedMessage().equals(message);
    }

    private boolean matchClosingEvent(ILoggingEvent event) {
        return event.getLevel().equals(Level.DEBUG)
            && event.getFormattedMessage().equals("Error notification consumed. ID " + MESSAGE_ID);
    }

    private List<ILoggingEvent> loggedEvents() {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
        TestAppender testAppender = (TestAppender) rootLogger.getAppender("TEST_APPENDER");
        return testAppender.getEvents();
    }
}
