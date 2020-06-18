package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConfigurationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectingException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ErrorMessageSenderTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String PO_BOX = "sample PO box";
    private static final String JURISDICTION = "jurisdiction";
    private static final String CONTAINER = "container";
    private static final String FILE_NAME = "file1.zip";
    private static final String MSG = "msg1";
    private static final long EVENT_ID = 2002L;
    private static final ErrorCode ERROR_CODE = ErrorCode.ERR_METAFILE_INVALID;
    private static final String BULK_SCAN_PROCESSOR = "bulk_scan_processor";
    public static final String UNMAPPED_CONTAINER = "unmapped_container";
    public static final String DETAILED_MESSAGE = "detailed message";

    private ErrorMessageSender errorMessageSender;

    @Mock
    private ServiceBusHelper notificationsQueueHelper;

    @Mock
    private ContainerMappings containerMappings;

    @Mock
    private OcrValidationException ocrValidationException;

    @Captor
    private ArgumentCaptor<ErrorMsg> argCaptor;

    @BeforeEach
    void setUp() {
        given(containerMappings.getMappings())
            .willReturn(singletonList(
                new ContainerMappings.Mapping(CONTAINER, JURISDICTION, PO_BOX, VALIDATION_URL, true, true)
            ));

        errorMessageSender = new ErrorMessageSender(
            notificationsQueueHelper,
            containerMappings
        );
    }

    @Test
    void should_send_error_message() {
        // given

        // when
        errorMessageSender.sendErrorMessage(
            FILE_NAME,
            CONTAINER,
            new OcrPresenceException(MSG),
            EVENT_ID,
            ERROR_CODE
        );

        // then
        verify(containerMappings).getMappings();
        verify(notificationsQueueHelper).sendMessage(argCaptor.capture());
        assertThat(argCaptor.getValue().eventId).isEqualTo(EVENT_ID);
        assertThat(argCaptor.getValue().zipFileName).isEqualTo(FILE_NAME);
        assertThat(argCaptor.getValue().jurisdiction).isEqualTo(CONTAINER);
        assertThat(argCaptor.getValue().poBox).isEqualTo(PO_BOX);
        assertThat(argCaptor.getValue().errorCode).isEqualTo(ERROR_CODE);
        assertThat(argCaptor.getValue().errorDescription).isEqualTo(MSG);
        assertThat(argCaptor.getValue().errorCode).isEqualTo(ERROR_CODE);
        assertThat(argCaptor.getValue().service).isEqualTo(BULK_SCAN_PROCESSOR);
        assertThat(argCaptor.getValue().container).isEqualTo(CONTAINER);
    }

    @Test
    void should_send_for_ocr_validation_error() {
        // given
        given(ocrValidationException.getDetailMessage()).willReturn(DETAILED_MESSAGE);

        // when
        errorMessageSender.sendErrorMessage(
            FILE_NAME,
            CONTAINER,
            ocrValidationException,
            EVENT_ID,
            ERROR_CODE
        );

        // then
        verify(containerMappings).getMappings();
        verify(notificationsQueueHelper).sendMessage(argCaptor.capture());
        assertThat(argCaptor.getValue().eventId).isEqualTo(EVENT_ID);
        assertThat(argCaptor.getValue().zipFileName).isEqualTo(FILE_NAME);
        assertThat(argCaptor.getValue().jurisdiction).isEqualTo(CONTAINER);
        assertThat(argCaptor.getValue().poBox).isEqualTo(PO_BOX);
        assertThat(argCaptor.getValue().errorCode).isEqualTo(ERROR_CODE);
        assertThat(argCaptor.getValue().errorDescription).isEqualTo(DETAILED_MESSAGE);
        assertThat(argCaptor.getValue().errorCode).isEqualTo(ERROR_CODE);
        assertThat(argCaptor.getValue().service).isEqualTo(BULK_SCAN_PROCESSOR);
        assertThat(argCaptor.getValue().container).isEqualTo(CONTAINER);
    }

    @Test
    void should_throw_if_no_container_mapping() {
        // given

        // when
        assertThatThrownBy(() ->
                               errorMessageSender.sendErrorMessage(
                                   FILE_NAME,
                                   UNMAPPED_CONTAINER,
                                   new OcrPresenceException(MSG),
                                   EVENT_ID,
                                   ERROR_CODE
                               )
        )
            .isInstanceOf(EnvelopeRejectingException.class)
            .hasMessageContaining("Error sending error notification to the queue."
                                      + "File name: " + FILE_NAME + " "
                                      + "Container: " + UNMAPPED_CONTAINER)
            .hasCauseInstanceOf(ConfigurationException.class);

        // then
        verify(containerMappings).getMappings();
        verifyNoInteractions(notificationsQueueHelper);
    }
}
